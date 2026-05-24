package com.sba.service;

import com.sba.model.AuditBlock;
import com.sba.repository.AuditBlockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class BlockchainAuditService {

    @Autowired private AuditBlockRepository blockRepo;

    public synchronized AuditBlock addBlock(String clientId, String action, String data) {
        AuditBlock prev = blockRepo.findTopByOrderByBlockIndexDesc().orElse(null);
        String prevHash = prev != null ? prev.getBlockHash() : "GENESIS_0000000000000000";
        long index = prev != null ? prev.getBlockIndex() + 1 : 0;
        LocalDateTime timestamp = LocalDateTime.now();
        long nonce = proofOfWork(index, prevHash, data, timestamp.toString());

        AuditBlock block = new AuditBlock();
        block.setBlockIndex(index);
        block.setPreviousHash(prevHash);
        block.setClientId(clientId);
        block.setAction(action);
        block.setData(data);
        block.setNonce(nonce);
        block.setTimestamp(timestamp);
        block.setBlockHash(calculateHash(index, prevHash, data, nonce, timestamp.toString()));

        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              BLOCKCHAIN AUDIT BLOCK CREATED                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Block Index  : " + block.getBlockIndex());
        System.out.println("║ Action       : " + block.getAction());
        System.out.println("║ Client       : " + block.getClientId());
        System.out.println("║ Timestamp    : " + block.getTimestamp());
        System.out.println("║ Nonce (PoW)  : " + block.getNonce());
        System.out.println("║ Block Hash   : " + block.getBlockHash());
        System.out.println("║ Previous Hash: " + block.getPreviousHash());
        System.out.println("║ Data         : " + block.getData());
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");

        return blockRepo.save(block);
    }

    public boolean validateChain() {
        List<AuditBlock> chain = blockRepo.findAllByOrderByBlockIndexAsc();
        System.out.println("\n[BLOCKCHAIN] Validating chain (" + chain.size() + " blocks)...");
        for (int i = 1; i < chain.size(); i++) {
            AuditBlock current = chain.get(i);
            AuditBlock previous = chain.get(i - 1);
            if (!current.getPreviousHash().equals(previous.getBlockHash())) {
                System.out.println("[BLOCKCHAIN] ✗ CHAIN BROKEN at block #" + current.getBlockIndex());
                return false;
            }
        }
        System.out.println("[BLOCKCHAIN] ✓ Chain integrity verified — all " + chain.size() + " blocks valid");
        return true;
    }

    public Map<String, Object> getChainStatus(String clientId) {
        List<AuditBlock> chain = clientId != null
                ? blockRepo.findByClientIdOrderByBlockIndexAsc(clientId)
                : blockRepo.findAllByOrderByBlockIndexAsc();
        Map<String, Object> status = new HashMap<>();
        status.put("totalBlocks", chain.size());
        status.put("chainValid", validateChain());
        status.put("latestBlock", chain.isEmpty() ? null : chain.get(chain.size() - 1));
        status.put("genesisBlock", chain.isEmpty() ? null : chain.get(0));
        return status;
    }

    public List<AuditBlock> getChainForClient(String clientId) {
        if (clientId == null) return blockRepo.findAllByOrderByBlockIndexAsc();
        return blockRepo.findByClientIdOrderByBlockIndexAsc(clientId);
    }

    public List<AuditBlock> getFullChain() {
        return blockRepo.findAllByOrderByBlockIndexAsc();
    }

    private long proofOfWork(long index, String prevHash, String data, String timestamp) {
        long nonce = 0;
        while (true) {
            String hash = calculateHash(index, prevHash, data, nonce, timestamp);
            if (hash.startsWith("00")) return nonce; // difficulty = 2 leading zeros
            nonce++;
        }
    }

    private String calculateHash(long index, String prevHash, String data, long nonce, String timestamp) {
        String input = index + prevHash + data + nonce + timestamp;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
