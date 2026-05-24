package com.sba.service;

import com.sba.model.*;
import com.sba.repository.*;
import com.sba.util.SBAUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SBAService {

    @Autowired private ClientRepository clientRepo;
    @Autowired private CloudFileRepository cloudFileRepo;
    @Autowired private BackupFileRepository backupFileRepo;
    @Autowired private SecondaryBackupRepository secondaryRepo;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private NodeStatusService nodeStatusService;
    @Autowired private FileNodeMappingRepository nodeMappingRepo;
    @Autowired private TertiaryBackupRepository tertiaryRepo;
    @Autowired private MetricsService metricsService;
    @Autowired private BlockchainAuditService blockchainAudit;
    @Autowired private com.sba.repository.FileVersionRepository fileVersionRepo;
    @Autowired private com.sba.repository.SharedFileRepository sharedFileRepo;
    @Autowired private com.sba.repository.TeamMemberRepository teamMemberRepo;

    public Client register(String username, String password, boolean isAdmin) {
        if (clientRepo.findByUsername(username).isPresent()) throw new RuntimeException("Username already taken.");
        String clientId = UUID.randomUUID().toString();
        byte[] rnd = new byte[32]; new SecureRandom().nextBytes(rnd);
        Client c = new Client();
        c.setClientId(clientId); c.setUsername(username);
        c.setPasswordHash(passwordEncoder.encode(password));
        c.setSeedBlock(SBAUtils.generateSeedBlock(clientId, rnd));
        c.setRole(isAdmin ? "ADMIN" : "USER");
        return clientRepo.save(c);
    }

    public Client register(String u, String p) { return register(u, p, false); }

    public Optional<Client> login(String username, String password) {
        return clientRepo.findByUsername(username)
            .filter(c -> c.isActive() && passwordEncoder.matches(password, c.getPasswordHash()))
            .map(c -> { c.setLastLogin(LocalDateTime.now()); return clientRepo.save(c); });
    }

    @Transactional
    public Map<String, Object> uploadFile(Client client, MultipartFile file) throws Exception {
        byte[] original = file.getBytes();
        String hash = SBAUtils.sha256Hash(original);
        Map<String, Object> result = new HashMap<>();

        // Deduplication
        Optional<CloudFile> existing = cloudFileRepo.findByClientAndFileHash(client, hash);
        if (existing.isPresent()) {
            result.put("duplicate", true);
            result.put("message", "Duplicate detected! Already backed up.");
            result.put("fileId", existing.get().getFileId());
            result.put("fileName", existing.get().getFileName());
            result.put("fileSize", SBAUtils.formatFileSize(existing.get().getFileSize()));
            result.put("fileHash", hash); result.put("xorTimeMs", 0L); result.put("aesTimeMs", 0L);
            result.put("savedBytes", SBAUtils.formatFileSize(original.length));
            return result;
        }

        // Compress
        byte[] toProcess; boolean compressed = false; long compSize = original.length; double ratio = 1.0;
        try {
            byte[] comp = SBAUtils.compress(original);
            if (comp.length < original.length * 0.95) { toProcess = comp; compressed = true; compSize = comp.length; ratio = SBAUtils.compressionRatio(original.length, comp.length); }
            else toProcess = original;
        } catch (Exception e) { toProcess = original; }

        // Multi-threaded XOR
        long xorStart = System.currentTimeMillis();
        byte[] xord = SBAUtils.xorMultiThreaded(toProcess, client.getSeedBlock());
        long xorTime = System.currentTimeMillis() - xorStart;

        // AES-256 on top of XOR
        long aesStart = System.currentTimeMillis();
        byte[] aesKey = SBAUtils.deriveAesKey(client.getSeedBlock());
        byte[] fileDash = SBAUtils.aesEncrypt(xord, aesKey);
        long aesTime = System.currentTimeMillis() - aesStart;

        CloudFile cf = new CloudFile();
        cf.setFileId(UUID.randomUUID().toString()); cf.setClient(client);
        cf.setFileName(SBAUtils.sanitizeFileName(file.getOriginalFilename())); cf.setFileType(file.getContentType());
        cf.setFileSize(original.length); cf.setCompressedSize(compSize);
        cf.setCompressed(compressed); cf.setFileHash(hash);
        cf.setUploadTimeMs(xorTime); cf.setCompressionRatio(ratio); cf.setFileData(original);
        cloudFileRepo.save(cf);

        // Primary backup (AES(XOR(data)))
        BackupFile b1 = new BackupFile();
        b1.setBackupId(UUID.randomUUID().toString()); b1.setCloudFile(cf); b1.setClient(client); b1.setFileDash(fileDash);
        backupFileRepo.save(b1);

        // Secondary backup (DoubleXOR)
        byte[] doubleXord = SBAUtils.doubleXor(toProcess, client.getSeedBlock(), client.getClientId());
        SecondaryBackup b2 = new SecondaryBackup();
        b2.setSecondaryId(UUID.randomUUID().toString()); b2.setCloudFile(cf); b2.setClient(client);
        b2.setFileDoubleXor(doubleXord); b2.setCompressedSize(compSize); b2.setCompressed(compressed);
        secondaryRepo.save(b2);

        result.put("duplicate", false); result.put("message", "Uploaded! GZIP+XOR+AES → 2 remote servers!");
        result.put("fileId", cf.getFileId()); result.put("fileName", cf.getFileName());
        result.put("fileSize", SBAUtils.formatFileSize(original.length));
        result.put("compressedSize", SBAUtils.formatFileSize(compSize));
        result.put("compressionRatio", ratio); result.put("wasCompressed", compressed);
        result.put("fileHash", hash); result.put("xorTimeMs", xorTime); result.put("aesTimeMs", aesTime);
        result.put("fileSizeBytes", original.length);

        // Create file-node mappings for distributed tracking
        nodeMappingRepo.deleteByFileId(cf.getFileId()); // clear if re-upload
        FileNodeMapping m1 = new FileNodeMapping();
        m1.setFileId(cf.getFileId()); m1.setNodeId(1);
        m1.setNodeLabel("Node A — Primary (AES-256 + XOR)");
        m1.setType("MAIN");
        m1.setFilePath("storage/node1/" + cf.getFileId() + "_" + file.getOriginalFilename() + ".enc");
        nodeMappingRepo.save(m1);

        FileNodeMapping m2 = new FileNodeMapping();
        m2.setFileId(cf.getFileId()); m2.setNodeId(2);
        m2.setNodeLabel("Node B — Secondary (Double XOR)");
        m2.setType("BACKUP");
        m2.setFilePath("storage/node2/" + cf.getFileId() + "_" + file.getOriginalFilename() + ".xor");
        nodeMappingRepo.save(m2);

        // Node C — Tertiary backup (Triple XOR)
        byte[] tripleXorData = SBAUtils.tripleXor(toProcess, client.getSeedBlock(), client.getClientId());
        TertiaryBackup tb = new TertiaryBackup();
        tb.setTertiaryId("TERTIARY_" + cf.getFileId());
        tb.setCloudFile(cf); tb.setClient(client);
        tb.setFileTripleXor(tripleXorData);
        tb.setCompressedSize(compSize); tb.setCompressed(compressed);
        tertiaryRepo.save(tb);

        FileNodeMapping m3 = new FileNodeMapping();
        m3.setFileId(cf.getFileId()); m3.setNodeId(3);
        m3.setNodeLabel("Node C — Tertiary (Triple XOR)");
        m3.setType("BACKUP");
        m3.setFilePath("storage/node3/" + cf.getFileId() + "_" + file.getOriginalFilename() + ".txor");
        nodeMappingRepo.save(m3);

        // Record metrics & blockchain audit
        metricsService.record("UPLOAD_XOR", original.length, xorTime);
        metricsService.record("UPLOAD_AES", original.length, aesTime);
        blockchainAudit.addBlock(client.getClientId(), "UPLOAD", cf.getFileId() + "|" + cf.getFileName() + "|" + hash);

        // File versioning
        com.sba.model.FileVersion fv = new com.sba.model.FileVersion();
        fv.setFileId(cf.getFileId());
        fv.setVersionNumber(1);
        fv.setFileHash(hash);
        fv.setFileSize(original.length);
        fv.setFileData(original);
        fv.setChangeSummary("Initial upload");
        fileVersionRepo.save(fv);

        return result;
    }

    public Map<String, Object> recoverPrimary(String fileId, Client client) throws Exception {
        // Check if node 1 data was explicitly deleted per-node
        boolean node1Deleted = nodeMappingRepo.findByFileIdAndNodeId(fileId, 1).map(m -> m.isDeleted()).orElse(false);
        // Node A FAILED or node 1 data deleted → auto-failover to Node B
        if (!nodeStatusService.isActive("A") || node1Deleted) {
            if (!nodeStatusService.isActive("B"))
                throw new RuntimeException("Both nodes are DOWN. Recovery unavailable.");
            Map<String, Object> r = doRecoverSecondary(fileId, client);
            r.put("failover", true);
            r.put("source", "FAILOVER → Secondary Server (Double XOR) [Node A was FAILED]");
            return r;
        }
        return doRecoverPrimary(fileId, client);
    }

    public Map<String, Object> recoverSecondary(String fileId, Client client) throws Exception {
        // Check if node 2 data was explicitly deleted per-node
        boolean node2Deleted = nodeMappingRepo.findByFileIdAndNodeId(fileId, 2).map(m -> m.isDeleted()).orElse(false);
        // Node B FAILED or node 2 data deleted → auto-failover to Node A
        if (!nodeStatusService.isActive("B") || node2Deleted) {
            if (!nodeStatusService.isActive("A"))
                throw new RuntimeException("Both nodes are DOWN. Recovery unavailable.");
            Map<String, Object> r = doRecoverPrimary(fileId, client);
            r.put("failover", true);
            r.put("source", "FAILOVER → Primary Server (AES-256 + XOR) [Node B was FAILED]");
            return r;
        }
        return doRecoverSecondary(fileId, client);
    }

    // Per-node delete (marks mapping as deleted, triggers failover on next recovery)
    @Transactional
    public void deleteFromNode(String fileId, int nodeId, Client client) {
        CloudFile cf = cloudFileRepo.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!cf.getClient().getClientId().equals(client.getClientId()))
            throw new RuntimeException("Access denied");
        nodeMappingRepo.findByFileIdAndNodeId(fileId, nodeId).ifPresent(m -> {
            m.setDeleted(true); m.setDeletedAt(LocalDateTime.now());
            nodeMappingRepo.save(m);
        });
    }

    public List<Map<String, Object>> getNodeMappings(String fileId, Client client) {
        CloudFile cf = cloudFileRepo.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!cf.getClient().getClientId().equals(client.getClientId()))
            throw new RuntimeException("Access denied");
        return nodeMappingRepo.findByFileId(fileId).stream().map(m -> {
            Map<String, Object> r = new HashMap<>();
            r.put("nodeId", m.getNodeId()); r.put("nodeLabel", m.getNodeLabel());
            r.put("type", m.getType()); r.put("filePath", m.getFilePath());
            r.put("deleted", m.isDeleted());
            r.put("deletedAt", m.getDeletedAt() != null ? m.getDeletedAt().toString().substring(0,19) : null);
            r.put("status", m.isDeleted() ? "DELETED" : "AVAILABLE");
            return r;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> doRecoverPrimary(String fileId, Client client) throws Exception {
        BackupFile b = backupFileRepo.findByCloudFileFileId(fileId).orElseThrow(() -> new RuntimeException("Backup not found."));
        CloudFile cf = b.getCloudFile();
        if (!cf.getClient().getClientId().equals(client.getClientId()))
            throw new RuntimeException("Access denied");
        long start = System.currentTimeMillis();
        byte[] aesKey = SBAUtils.deriveAesKey(client.getSeedBlock());
        byte[] decAes = SBAUtils.aesDecrypt(b.getFileDash(), aesKey);
        byte[] xorResult = SBAUtils.xorMultiThreaded(decAes, client.getSeedBlock());
        byte[] recovered = cf.isCompressed() ? SBAUtils.decompress(xorResult) : xorResult;
        long t = System.currentTimeMillis() - start;
        metricsService.record("RECOVER_PRIMARY", recovered.length, t);
        String rHash = SBAUtils.sha256Hash(recovered);
        Map<String, Object> r = new HashMap<>();
        r.put("data", recovered); r.put("verified", rHash.equals(cf.getFileHash()));
        r.put("originalHash", cf.getFileHash()); r.put("recoveredHash", rHash);
        r.put("xorTimeMs", t); r.put("fileName", cf.getFileName()); r.put("fileType", cf.getFileType());
        r.put("source", "Primary Server (AES-256 + XOR)");
        r.put("failover", false);
        return r;
    }

    private Map<String, Object> doRecoverSecondary(String fileId, Client client) throws Exception {
        SecondaryBackup b = secondaryRepo.findByCloudFileFileId(fileId).orElseThrow(() -> new RuntimeException("Secondary not found."));
        CloudFile cf = b.getCloudFile();
        if (!cf.getClient().getClientId().equals(client.getClientId()))
            throw new RuntimeException("Access denied");
        long start = System.currentTimeMillis();
        byte[] xorResult = SBAUtils.doubleXor(b.getFileDoubleXor(), client.getSeedBlock(), client.getClientId());
        byte[] recovered = cf.isCompressed() ? SBAUtils.decompress(xorResult) : xorResult;
        long t = System.currentTimeMillis() - start;
        metricsService.record("RECOVER_SECONDARY", recovered.length, t);
        String rHash = SBAUtils.sha256Hash(recovered);
        Map<String, Object> r = new HashMap<>();
        r.put("data", recovered); r.put("verified", rHash.equals(cf.getFileHash()));
        r.put("originalHash", cf.getFileHash()); r.put("recoveredHash", rHash);
        r.put("xorTimeMs", t); r.put("fileName", cf.getFileName()); r.put("fileType", cf.getFileType());
        r.put("source", "Secondary Server (Double XOR)");
        r.put("failover", false);
        return r;
    }

    // ── NODE C RECOVERY ──────────────────────────────────────────────────────
    public Map<String, Object> recoverTertiary(String fileId, Client client) throws Exception {
        boolean node3Deleted = nodeMappingRepo.findByFileIdAndNodeId(fileId, 3).map(m -> m.isDeleted()).orElse(false);
        if (!nodeStatusService.isActive("C") || node3Deleted) {
            // Fallback to Node A
            if (nodeStatusService.isActive("A") && !nodeMappingRepo.findByFileIdAndNodeId(fileId, 1).map(m -> m.isDeleted()).orElse(false)) {
                Map<String, Object> r = doRecoverPrimary(fileId, client);
                r.put("failover", true);
                r.put("source", "FAILOVER → Node A (AES-256 + XOR) [Node C was FAILED]");
                return r;
            }
            throw new RuntimeException("Node C FAILED and no fallback available.");
        }
        TertiaryBackup b = tertiaryRepo.findByCloudFileFileId(fileId).orElseThrow(() -> new RuntimeException("Tertiary backup not found."));
        CloudFile cf = b.getCloudFile();
        if (!cf.getClient().getClientId().equals(client.getClientId()))
            throw new RuntimeException("Access denied");
        long start = System.currentTimeMillis();
        byte[] xorResult = SBAUtils.tripleXor(b.getFileTripleXor(), client.getSeedBlock(), client.getClientId());
        byte[] recovered = cf.isCompressed() ? SBAUtils.decompress(xorResult) : xorResult;
        long t = System.currentTimeMillis() - start;
        String rHash = SBAUtils.sha256Hash(recovered);
        Map<String, Object> r = new HashMap<>();
        r.put("data", recovered); r.put("verified", rHash.equals(cf.getFileHash()));
        r.put("originalHash", cf.getFileHash()); r.put("recoveredHash", rHash);
        r.put("xorTimeMs", t); r.put("fileName", cf.getFileName()); r.put("fileType", cf.getFileType());
        r.put("source", "Tertiary Server (Triple XOR)");
        r.put("failover", false);
        return r;
    }

    public Map<String, Object> verifyFile(String fileId, Client client) throws Exception {
        Map<String, Object> r = recoverPrimary(fileId, client); r.remove("data"); return r;
    }

    public List<CloudFile> getClientFiles(Client client) { return cloudFileRepo.findByClientOrderByUploadedAtDesc(client); }
    public Optional<CloudFile> getFile(String fileId) { return cloudFileRepo.findById(fileId); }

    public boolean isFileSharedWithUser(String fileId, String clientId) {
        List<com.sba.model.SharedFile> shares = sharedFileRepo.findByFileId(fileId);
        for (com.sba.model.SharedFile sf : shares) {
            if (teamMemberRepo.findByOrgIdAndClientId(sf.getOrgId(), clientId).isPresent()) return true;
        }
        return false;
    }

    @Transactional
    public void deleteFile(String fileId, Client client) {
        CloudFile cf = cloudFileRepo.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!cf.getClient().getClientId().equals(client.getClientId()))
            throw new RuntimeException("Access denied");
        blockchainAudit.addBlock(client.getClientId(), "DELETE", fileId);
        secondaryRepo.deleteByCloudFileFileId(fileId);
        backupFileRepo.deleteByCloudFileFileId(fileId);
        tertiaryRepo.deleteByCloudFileFileId(fileId);
        nodeMappingRepo.deleteByFileId(fileId);
        cloudFileRepo.deleteById(fileId);
    }

    public List<Client> getAllUsers() { return clientRepo.findAll(); }
    public void toggleUserActive(String id) { clientRepo.findById(id).ifPresent(c -> { c.setActive(!c.isActive()); clientRepo.save(c); }); }
    public void deleteUser(String id) { clientRepo.deleteById(id); }

    public Map<String, Object> getSystemStats() {
        List<Client> users = clientRepo.findAll();
        List<CloudFile> files = cloudFileRepo.findAll();
        long total = files.stream().mapToLong(CloudFile::getFileSize).sum();
        long comp = files.stream().mapToLong(f -> f.getCompressedSize() > 0 ? f.getCompressedSize() : f.getFileSize()).sum();
        double avgXor = files.stream().mapToLong(CloudFile::getUploadTimeMs).average().orElse(0);
        Map<String, Object> s = new HashMap<>();
        s.put("totalUsers", users.size()); s.put("activeUsers", users.stream().filter(Client::isActive).count());
        s.put("totalFiles", files.size()); s.put("totalSize", SBAUtils.formatFileSize(total));
        s.put("totalSizeBytes", total); s.put("spaceSaved", SBAUtils.formatFileSize(Math.max(0, total - comp)));
        s.put("avgXorTimeMs", Math.round(avgXor));
        s.put("duplicatesSaved", files.stream().filter(CloudFile::isDuplicate).count());
        s.put("compressedFiles", files.stream().filter(CloudFile::isCompressed).count());
        List<Map<String, Object>> perf = files.stream().sorted(Comparator.comparingLong(CloudFile::getFileSize)).map(f -> {
            Map<String, Object> p = new HashMap<>();
            p.put("fileName", f.getFileName()); p.put("sizeKB", Math.round(f.getFileSize() / 1024.0 * 10) / 10.0);
            p.put("xorTimeMs", f.getUploadTimeMs()); p.put("compressionRatio", f.getCompressionRatio());
            p.put("compressed", f.isCompressed()); return p;
        }).collect(Collectors.toList());
        s.put("performanceData", perf);
        return s;
    }
}
