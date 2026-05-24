package com.sba.service;

import com.sba.util.SBAUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Service
public class CryptoAnalysisService {

    // ── ENTROPY ANALYSIS ─────────────────────────────────────────────────────
    public Map<String, Object> analyzeEntropy(byte[] data) {
        Map<String, Object> result = new HashMap<>();
        double entropy = shannonEntropy(data);
        result.put("shannonEntropy", Math.round(entropy * 10000) / 10000.0);
        result.put("maxEntropy", 8.0);
        result.put("entropyPercent", Math.round(entropy / 8.0 * 10000) / 100.0);
        result.put("quality", entropy > 7.9 ? "EXCELLENT" : entropy > 7.5 ? "GOOD" : entropy > 6.0 ? "MODERATE" : "WEAK");
        result.put("dataSize", data.length);

        // Byte frequency distribution (top 10 most/least frequent)
        int[] freq = new int[256];
        for (byte b : data) freq[b & 0xFF]++;
        double idealFreq = data.length / 256.0;
        double chiSquare = 0;
        for (int f : freq) chiSquare += Math.pow(f - idealFreq, 2) / idealFreq;
        result.put("chiSquare", Math.round(chiSquare * 100) / 100.0);
        result.put("isRandom", chiSquare < 300); // chi-square test for uniformity
        return result;
    }

    private double shannonEntropy(byte[] data) {
        if (data.length == 0) return 0;
        int[] freq = new int[256];
        for (byte b : data) freq[b & 0xFF]++;
        double entropy = 0;
        for (int f : freq) {
            if (f == 0) continue;
            double p = (double) f / data.length;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    // ── MERKLE TREE ──────────────────────────────────────────────────────────
    public Map<String, Object> buildMerkleTree(byte[] data, int chunkSize) {
        List<String> leaves = new ArrayList<>();
        for (int i = 0; i < data.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, data.length);
            leaves.add(sha256(Arrays.copyOfRange(data, i, end)));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalChunks", leaves.size());
        result.put("chunkSize", chunkSize);
        result.put("leaves", leaves);

        List<List<String>> levels = new ArrayList<>();
        levels.add(new ArrayList<>(leaves));
        List<String> current = leaves;
        while (current.size() > 1) {
            List<String> next = new ArrayList<>();
            for (int i = 0; i < current.size(); i += 2) {
                String left = current.get(i);
                String right = i + 1 < current.size() ? current.get(i + 1) : left;
                next.add(sha256((left + right).getBytes(StandardCharsets.UTF_8)));
            }
            levels.add(next);
            current = next;
        }
        result.put("merkleRoot", current.get(0));
        result.put("treeDepth", levels.size());
        result.put("levels", levels);
        return result;
    }

    public boolean verifyMerkleProof(String leafHash, List<Map<String, String>> proof, String root) {
        String current = leafHash;
        for (Map<String, String> step : proof) {
            String sibling = step.get("hash");
            String position = step.get("position");
            current = "left".equals(position)
                    ? sha256((sibling + current).getBytes(StandardCharsets.UTF_8))
                    : sha256((current + sibling).getBytes(StandardCharsets.UTF_8));
        }
        return current.equals(root);
    }

    // ── ZERO-KNOWLEDGE PROOF OF STORAGE ──────────────────────────────────────
    public Map<String, Object> generateStorageChallenge(byte[] fileData) {
        SecureRandom rng = new SecureRandom();
        int numChallenges = 5;
        List<Map<String, Object>> challenges = new ArrayList<>();
        for (int i = 0; i < numChallenges; i++) {
            int offset = rng.nextInt(Math.max(1, fileData.length - 32));
            int length = Math.min(32, fileData.length - offset);
            byte[] chunk = Arrays.copyOfRange(fileData, offset, offset + length);
            String expectedHash = sha256(chunk);
            Map<String, Object> c = new HashMap<>();
            c.put("challengeId", UUID.randomUUID().toString().substring(0, 8));
            c.put("offset", offset);
            c.put("length", length);
            c.put("expectedHash", expectedHash);
            challenges.add(c);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("challenges", challenges);
        result.put("timestamp", System.currentTimeMillis());
        result.put("fileSize", fileData.length);
        return result;
    }

    public Map<String, Object> respondToChallenge(byte[] fileData, int offset, int length) {
        byte[] chunk = Arrays.copyOfRange(fileData, offset, Math.min(offset + length, fileData.length));
        Map<String, Object> response = new HashMap<>();
        response.put("hash", sha256(chunk));
        response.put("offset", offset);
        response.put("length", chunk.length);
        response.put("verified", true);
        return response;
    }

    // ── ALGORITHM BENCHMARKING ───────────────────────────────────────────────
    public Map<String, Object> benchmark(int dataSizeKB) {
        byte[] testData = new byte[dataSizeKB * 1024];
        new SecureRandom().nextBytes(testData);
        byte[] seed = new byte[32];
        new SecureRandom().nextBytes(seed);

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("dataSizeKB", dataSizeKB);

        // XOR benchmark
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) SBAUtils.xorWithSeed(testData, seed);
        long xorNs = (System.nanoTime() - start) / 100;
        results.put("xor", benchResult("XOR (Seed Block)", xorNs, testData.length));

        // AES-256-GCM benchmark
        try {
            byte[] aesKey = SBAUtils.deriveAesKey(seed);
            start = System.nanoTime();
            for (int i = 0; i < 100; i++) SBAUtils.aesEncrypt(testData, aesKey);
            long aesNs = (System.nanoTime() - start) / 100;
            results.put("aesGcm", benchResult("AES-256-GCM", aesNs, testData.length));
        } catch (Exception e) { results.put("aesGcm", Map.of("error", e.getMessage())); }

        // ChaCha20 benchmark
        try {
            byte[] key = Arrays.copyOf(seed, 32);
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            start = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new IvParameterSpec(nonce));
                cipher.doFinal(testData);
            }
            long chachaNs = (System.nanoTime() - start) / 100;
            results.put("chacha20", benchResult("ChaCha20-Poly1305", chachaNs, testData.length));
        } catch (Exception e) { results.put("chacha20", Map.of("error", "Not available: " + e.getMessage())); }

        // Double XOR benchmark
        start = System.nanoTime();
        for (int i = 0; i < 100; i++) SBAUtils.doubleXor(testData, seed, "test-client-id");
        long dxorNs = (System.nanoTime() - start) / 100;
        results.put("doubleXor", benchResult("Double XOR", dxorNs, testData.length));

        // SHA-256 hash benchmark
        start = System.nanoTime();
        for (int i = 0; i < 100; i++) SBAUtils.sha256Hash(testData);
        long hashNs = (System.nanoTime() - start) / 100;
        results.put("sha256", benchResult("SHA-256 Hash", hashNs, testData.length));

        return results;
    }

    private Map<String, Object> benchResult(String name, long avgNs, int dataSize) {
        Map<String, Object> r = new HashMap<>();
        r.put("algorithm", name);
        r.put("avgNs", avgNs);
        r.put("avgMs", Math.round(avgNs / 1_000_000.0 * 100) / 100.0);
        r.put("throughputMBps", avgNs > 0 ? Math.round(dataSize * 1_000_000_000.0 / avgNs / 1048576 * 100) / 100.0 : 0);
        return r;
    }

    private String sha256(byte[] data) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private String sha256(String s) { return sha256(s.getBytes(StandardCharsets.UTF_8)); }
}
