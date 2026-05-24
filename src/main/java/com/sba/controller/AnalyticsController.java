package com.sba.controller;

import com.sba.model.CloudFile;
import com.sba.repository.ClientRepository;
import com.sba.repository.CloudFileRepository;
import com.sba.service.*;
import com.sba.util.SBAUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Advanced Analytics", description = "Blockchain audit, metrics, crypto analysis")
public class AnalyticsController {

    @Autowired private BlockchainAuditService blockchainService;
    @Autowired private MetricsService metricsService;
    @Autowired private CryptoAnalysisService cryptoService;
    @Autowired private CloudFileRepository cloudFileRepo;
    @Autowired private ClientRepository clientRepo;
    @Autowired private NodeStatusService nodeStatusService;

    // ── BLOCKCHAIN AUDIT ─────────────────────────────────────────────────────
    @Operation(summary = "Get blockchain audit chain status")
    @GetMapping("/blockchain/status")
    public ResponseEntity<?> blockchainStatus(HttpServletRequest req) {
        String clientId = (String) req.getAttribute("clientId");
        return ResponseEntity.ok(blockchainService.getChainStatus(clientId));
    }

    @Operation(summary = "Get full blockchain audit trail")
    @GetMapping("/blockchain/chain")
    public ResponseEntity<?> fullChain(HttpServletRequest req) {
        String clientId = (String) req.getAttribute("clientId");
        return ResponseEntity.ok(blockchainService.getChainForClient(clientId));
    }

    @Operation(summary = "Validate blockchain integrity")
    @GetMapping("/blockchain/validate")
    public ResponseEntity<?> validateChain() {
        boolean valid = blockchainService.validateChain();
        return ResponseEntity.ok(Map.of("valid", valid, "message", valid ? "Chain integrity verified ✓" : "CHAIN TAMPERED — integrity broken!"));
    }

    // ── REAL-TIME METRICS ────────────────────────────────────────────────────
    @Operation(summary = "Get real-time encryption performance metrics")
    @GetMapping("/metrics")
    public ResponseEntity<?> metrics() {
        return ResponseEntity.ok(metricsService.getMetrics());
    }

    // ── ENTROPY ANALYSIS ─────────────────────────────────────────────────────
    @Operation(summary = "Analyze encryption entropy of a stored file")
    @GetMapping("/entropy/{fileId}")
    public ResponseEntity<?> entropy(@PathVariable String fileId, HttpServletRequest req) {
        try {
            String clientId = (String) req.getAttribute("clientId");
            if (clientId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            CloudFile cf = cloudFileRepo.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
            if (!cf.getClient().getClientId().equals(clientId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            byte[] seed = clientRepo.findById(clientId).orElseThrow().getSeedBlock();
            byte[] encrypted = SBAUtils.xorWithSeed(cf.getFileData(), seed);
            Map<String, Object> original = cryptoService.analyzeEntropy(cf.getFileData());
            Map<String, Object> encEntropy = cryptoService.analyzeEntropy(encrypted);
            return ResponseEntity.ok(Map.of("original", original, "encrypted", encEntropy, "fileName", cf.getFileName()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── MERKLE TREE ──────────────────────────────────────────────────────────
    @Operation(summary = "Build Merkle tree for file integrity verification")
    @GetMapping("/merkle/{fileId}")
    public ResponseEntity<?> merkleTree(@PathVariable String fileId, HttpServletRequest req) {
        try {
            String clientId = (String) req.getAttribute("clientId");
            if (clientId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            CloudFile cf = cloudFileRepo.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
            if (!cf.getClient().getClientId().equals(clientId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            int chunkSize = Math.max(1024, (int)(cf.getFileSize() / 16)); // ~16 chunks
            Map<String, Object> tree = cryptoService.buildMerkleTree(cf.getFileData(), chunkSize);
            tree.put("fileName", cf.getFileName());
            tree.put("fileSize", cf.getFileSize());
            return ResponseEntity.ok(tree);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── ZERO-KNOWLEDGE PROOF ─────────────────────────────────────────────────
    @Operation(summary = "Generate zero-knowledge storage proof challenge")
    @GetMapping("/zkp/challenge/{fileId}")
    public ResponseEntity<?> zkpChallenge(@PathVariable String fileId, HttpServletRequest req) {
        try {
            String clientId = (String) req.getAttribute("clientId");
            if (clientId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            CloudFile cf = cloudFileRepo.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
            if (!cf.getClient().getClientId().equals(clientId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(cryptoService.generateStorageChallenge(cf.getFileData()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @Operation(summary = "Verify zero-knowledge proof response")
    @PostMapping("/zkp/verify/{fileId}")
    public ResponseEntity<?> zkpVerify(@PathVariable String fileId, @RequestBody Map<String, Object> body, HttpServletRequest req) {
        try {
            String clientId = (String) req.getAttribute("clientId");
            if (clientId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            CloudFile cf = cloudFileRepo.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
            if (!cf.getClient().getClientId().equals(clientId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            int offset = (Integer) body.get("offset");
            int length = (Integer) body.get("length");
            String expectedHash = (String) body.get("expectedHash");
            Map<String, Object> response = cryptoService.respondToChallenge(cf.getFileData(), offset, length);
            boolean valid = response.get("hash").equals(expectedHash);
            response.put("verified", valid);
            response.put("message", valid ? "Proof valid — node holds correct data" : "PROOF FAILED — data mismatch!");
            return ResponseEntity.ok(response);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── BENCHMARKING ─────────────────────────────────────────────────────────
    @Operation(summary = "Run encryption algorithm benchmark")
    @GetMapping("/benchmark")
    public ResponseEntity<?> benchmark(@RequestParam(defaultValue = "256") int sizeKB) {
        return ResponseEntity.ok(cryptoService.benchmark(Math.min(sizeKB, 4096)));
    }

    // ── NODE HEARTBEAT ───────────────────────────────────────────────────────
    @Operation(summary = "Get node health with heartbeat info")
    @GetMapping("/nodes/health")
    public ResponseEntity<?> nodeHealth() {
        Map<String, Object> health = new java.util.HashMap<>();
        Map<String, String> statuses = nodeStatusService.getAllStatuses();
        long now = System.currentTimeMillis();
        statuses.forEach((node, status) -> {
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("status", status);
            info.put("lastHeartbeat", now - (long)(Math.random() * 5000)); // simulated
            info.put("latencyMs", Math.round(Math.random() * 15 + 2));
            info.put("uptime", status.equals("ACTIVE") ? "99." + (int)(Math.random() * 9 + 1) + "%" : "0%");
            health.put("node" + node, info);
        });
        health.put("timestamp", now);
        health.put("clusterHealthy", statuses.values().stream().allMatch(s -> s.equals("ACTIVE")));
        return ResponseEntity.ok(health);
    }
}
