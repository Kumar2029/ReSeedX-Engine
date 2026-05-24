package com.sba.controller;

import com.sba.model.Client;
import com.sba.model.CloudFile;
import com.sba.repository.ClientRepository;
import com.sba.service.AuditService;
import com.sba.service.SBAService;
import com.sba.util.SBAUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management")
public class FileController {

    @Autowired private SBAService sbaService;
    @Autowired private ClientRepository clientRepo;
    @Autowired private AuditService audit;
    @Autowired private com.sba.repository.FileVersionRepository fileVersionRepo;

    private Client getClient(HttpServletRequest req) {
        String id = (String) req.getAttribute("clientId");
        if (id == null) throw new RuntimeException("Unauthorized - JWT required");
        return clientRepo.findById(id).orElseThrow(() -> new RuntimeException("Client not found"));
    }

    private ResponseEntity<?> fileResponse(byte[] data, String name, String type) throws Exception {
        String enc = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        String ct = type != null ? type : "application/octet-stream";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"; filename*=UTF-8''" + enc)
            .contentType(MediaType.parseMediaType(ct)).body(data);
    }

    @Operation(summary = "Upload with GZIP+MultiThreadedXOR+AES-256 dual-cloud backup")
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No file"));
            if (file.getSize() > 500 * 1024 * 1024) return ResponseEntity.badRequest().body(Map.of("error", "Max 500MB"));
            Client c = getClient(req);
            long start = System.currentTimeMillis();
            Map<String, Object> result = sbaService.uploadFile(c, file);
            audit.log(c.getClientId(), c.getUsername(), "UPLOAD", file.getOriginalFilename(), file.getSize(), result.get("message").toString(), true, req.getRemoteAddr(), System.currentTimeMillis() - start);
            return ResponseEntity.ok(result);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(HttpServletRequest req,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {
        try {
            Client c = getClient(req);
            size = Math.min(size, 200);
            List<CloudFile> allFiles = sbaService.getClientFiles(c);
            int start = page * size;
            int end = Math.min(start + size, allFiles.size());
            List<Map<String, Object>> result = (start >= allFiles.size() ? List.<CloudFile>of() : allFiles.subList(start, end)).stream().map(f -> {
                Map<String, Object> m = new HashMap<>();
                m.put("fileId", f.getFileId());
                m.put("fileName", f.getFileName() != null ? f.getFileName() : "unknown");
                m.put("fileType", f.getFileType() != null ? f.getFileType() : "application/octet-stream");
                m.put("fileSize", SBAUtils.formatFileSize(f.getFileSize()));
                m.put("fileSizeBytes", f.getFileSize());
                m.put("compressedSize", SBAUtils.formatFileSize(f.getCompressedSize() > 0 ? f.getCompressedSize() : f.getFileSize()));
                m.put("compressionRatio", f.getCompressionRatio());
                m.put("compressed", f.isCompressed());
                m.put("fileHash", f.getFileHash() != null ? f.getFileHash() : "");
                m.put("xorTimeMs", f.getUploadTimeMs());
                m.put("uploadedAt", f.getUploadedAt().toString().replace("T", " ").substring(0, 19));
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("files", result, "page", page, "size", size, "totalFiles", allFiles.size(), "totalPages", (int) Math.ceil((double) allFiles.size() / size)));
        } catch (Exception e) { return ResponseEntity.status(401).body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> download(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            CloudFile cf = sbaService.getFile(fileId).orElseThrow(() -> new RuntimeException("Not found"));
            // Allow if owner OR if file is shared with a team the user belongs to
            if (!cf.getClient().getClientId().equals(c.getClientId()) && !sbaService.isFileSharedWithUser(fileId, c.getClientId()))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            audit.log(c.getClientId(), c.getUsername(), "DOWNLOAD", cf.getFileName(), cf.getFileSize(), "Main cloud", true, req.getRemoteAddr(), null);
            return fileResponse(cf.getFileData(), cf.getFileName() != null ? cf.getFileName() : "file", cf.getFileType());
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/recover/{fileId}")
    public ResponseEntity<?> recoverPrimary(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            Map<String, Object> r = sbaService.recoverPrimary(fileId, c);
            audit.log(c.getClientId(), c.getUsername(), "RECOVER", r.get("fileName").toString(), null, "Server1 AES+XOR verified=" + r.get("verified"), true, req.getRemoteAddr(), (Long) r.get("xorTimeMs"));
            return fileResponse((byte[]) r.get("data"), "RECOVERED_" + r.get("fileName"), (String) r.get("fileType"));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/recover/{fileId}/info")
    public ResponseEntity<?> recoverPrimaryInfo(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            Map<String, Object> r = sbaService.recoverPrimary(fileId, c);
            r.remove("data");
            return ResponseEntity.ok(r);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/recover2/{fileId}")
    public ResponseEntity<?> recoverSecondary(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            Map<String, Object> r = sbaService.recoverSecondary(fileId, c);
            audit.log(c.getClientId(), c.getUsername(), "RECOVER2", r.get("fileName").toString(), null, "Server2 DoubleXOR", true, req.getRemoteAddr(), (Long) r.get("xorTimeMs"));
            return fileResponse((byte[]) r.get("data"), "RECOVERED2_" + r.get("fileName"), (String) r.get("fileType"));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/recover2/{fileId}/info")
    public ResponseEntity<?> recoverSecondaryInfo(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            Map<String, Object> r = sbaService.recoverSecondary(fileId, c);
            r.remove("data");
            return ResponseEntity.ok(r);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/verify/{fileId}")
    public ResponseEntity<?> verify(@PathVariable String fileId, HttpServletRequest req) {
        try { Client c = getClient(req); return ResponseEntity.ok(sbaService.verifyFile(fileId, c)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> delete(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            sbaService.deleteFile(fileId, c);
            audit.log(c.getClientId(), c.getUsername(), "DELETE", fileId, null, "All servers", true, req.getRemoteAddr(), null);
            return ResponseEntity.ok(Map.of("message", "Deleted from all servers"));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(HttpServletRequest req) {
        try {
            Client c = getClient(req);
            List<CloudFile> files = sbaService.getClientFiles(c);
            long total = files.stream().mapToLong(CloudFile::getFileSize).sum();
            long comp = files.stream().mapToLong(f -> f.getCompressedSize() > 0 ? f.getCompressedSize() : f.getFileSize()).sum();
            double avg = files.stream().mapToLong(CloudFile::getUploadTimeMs).average().orElse(0);
            Map<String, Object> m = new HashMap<>();
            m.put("totalFiles", files.size()); m.put("totalSize", SBAUtils.formatFileSize(total));
            m.put("spaceSaved", SBAUtils.formatFileSize(Math.max(0, total - comp)));
            m.put("username", c.getUsername()); m.put("avgXorTimeMs", Math.round(avg));
            return ResponseEntity.ok(m);
        } catch (Exception e) { return ResponseEntity.status(e.getMessage()!=null&&e.getMessage().contains("Unauthorized")?401:500).body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/audit")
    public ResponseEntity<?> auditLog(HttpServletRequest req) {
        try {
            Client c = getClient(req);
            return ResponseEntity.ok(audit.getByClient(c.getClientId()));
        } catch (Exception e) { return ResponseEntity.status(401).body(Map.of("error", e.getMessage())); }
    }

    /** Delete file from a specific node only (marks mapping deleted, triggers failover) */
    @DeleteMapping("/{fileId}/node/{nodeId}")
    public ResponseEntity<?> deleteFromNode(@PathVariable String fileId, @PathVariable int nodeId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            if (nodeId != 1 && nodeId != 2 && nodeId != 3) return ResponseEntity.badRequest().body(Map.of("error", "nodeId must be 1, 2 or 3"));
            sbaService.deleteFromNode(fileId, nodeId, c);
            String nodeLabel = nodeId == 1 ? "Node A (Primary)" : nodeId == 2 ? "Node B (Secondary)" : "Node C (Tertiary)";
            audit.log(c.getClientId(), c.getUsername(), "DELETE_NODE", fileId, null, "Deleted from " + nodeLabel, true, req.getRemoteAddr(), null);
            return ResponseEntity.ok(Map.of(
                "message", "Deleted from " + nodeLabel,
                "fileId", fileId,
                "nodeId", nodeId,
                "status", "DELETED"
            ));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** Recover from Node C (Tertiary Triple XOR) */
    @GetMapping("/recover3/{fileId}")
    public ResponseEntity<?> recover3(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            Map<String, Object> result = sbaService.recoverTertiary(fileId, c);
            byte[] data = (byte[]) result.get("data");
            String fn = (String) result.get("fileName");
            String ft = (String) result.get("fileType");
            audit.log(c.getClientId(), c.getUsername(), "RECOVER3", fn, null, result.get("source").toString(), true, req.getRemoteAddr(), null);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(ft != null ? MediaType.parseMediaType(ft) : MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/recover3/{fileId}/info")
    public ResponseEntity<?> recover3Info(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            Map<String, Object> r = sbaService.recoverTertiary(fileId, c);
            r.remove("data");
            return ResponseEntity.ok(r);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** Get node distribution/mapping for a file */
    @GetMapping("/mappings/{fileId}")
    public ResponseEntity<?> nodeMappings(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            return ResponseEntity.ok(sbaService.getNodeMappings(fileId, c));
        } catch (Exception e) { return ResponseEntity.status(401).body(Map.of("error", e.getMessage())); }
    }

    /** Get file version history */
    @GetMapping("/versions/{fileId}")
    public ResponseEntity<?> versions(@PathVariable String fileId, HttpServletRequest req) {
        try {
            Client c = getClient(req);
            CloudFile cf = sbaService.getFile(fileId).orElseThrow(() -> new RuntimeException("Not found"));
            if (!cf.getClient().getClientId().equals(c.getClientId()))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            var versions = fileVersionRepo.findByFileIdOrderByVersionNumberDesc(fileId).stream().map(v -> {
                Map<String, Object> m = new HashMap<>();
                m.put("version", v.getVersionNumber());
                m.put("fileHash", v.getFileHash());
                m.put("fileSize", SBAUtils.formatFileSize(v.getFileSize()));
                m.put("changeSummary", v.getChangeSummary());
                m.put("createdAt", v.getCreatedAt().toString().replace("T", " ").substring(0, 19));
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(versions);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
