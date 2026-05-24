package com.sba.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.zip.*;

public class SBAUtils {

    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    private static final int THREAD_POOL = 4;
    private static final ExecutorService XOR_POOL = Executors.newFixedThreadPool(THREAD_POOL);

    // ── CORE XOR ──────────────────────────────────────────────────────────────
    public static byte[] generateSeedBlock(String clientId, byte[] randomNumber) {
        byte[] cb = clientId.getBytes(StandardCharsets.UTF_8);
        int len = Math.max(cb.length, randomNumber.length);
        byte[] seed = new byte[len];
        for (int i = 0; i < len; i++) {
            seed[i] = (byte)((i < cb.length ? cb[i] : 0) ^ (i < randomNumber.length ? randomNumber[i] : 0));
        }
        return seed;
    }

    public static byte[] xorWithSeed(byte[] data, byte[] seed) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++)
            result[i] = (byte)(data[i] ^ seed[i % seed.length]);
        return result;
    }

    // ── PHASE 3: MULTI-THREADED XOR (splits into chunks, processes in parallel) ─
    public static byte[] xorMultiThreaded(byte[] data, byte[] seed) throws Exception {
        if (data.length < CHUNK_SIZE) return xorWithSeed(data, seed); // small file: no threading needed
        int numChunks = (int) Math.ceil((double) data.length / CHUNK_SIZE);
        byte[][] results = new byte[numChunks][];
        Future<?>[] futures = new Future[numChunks];
        for (int c = 0; c < numChunks; c++) {
            final int chunk = c;
            final int start = chunk * CHUNK_SIZE;
            final int end = Math.min(start + CHUNK_SIZE, data.length);
            futures[c] = XOR_POOL.submit(() -> {
                byte[] chunkData = Arrays.copyOfRange(data, start, end);
                results[chunk] = xorWithSeed(chunkData, seed);
            });
        }
        for (Future<?> f : futures) f.get();
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        for (byte[] r : results) out.write(r);
        return out.toByteArray();
    }

    // ── PHASE 2: DOUBLE XOR ────────────────────────────────────────────────────
    public static byte[] doubleXor(byte[] data, byte[] seed, String clientId) {
        byte[] first = xorWithSeed(data, seed);
        byte[] cb = clientId.getBytes(StandardCharsets.UTF_8);
        byte[] rev = new byte[cb.length];
        for (int i = 0; i < cb.length; i++) rev[i] = cb[cb.length - 1 - i];
        return xorWithSeed(first, rev);
    }

    // Triple XOR: File XOR Seed XOR clientId XOR reversed(Seed) — Node C strategy
    public static byte[] tripleXor(byte[] data, byte[] seed, String clientId) {
        byte[] step1 = xorWithSeed(data, seed);
        byte[] cb = clientId.getBytes(StandardCharsets.UTF_8);
        byte[] step2 = xorWithSeed(step1, cb);
        byte[] revSeed = new byte[seed.length];
        for (int i = 0; i < seed.length; i++) revSeed[i] = seed[seed.length - 1 - i];
        return xorWithSeed(step2, revSeed);
    }

    // ── PHASE 3: AES-256-GCM ENCRYPTION ─────────────────────────────────────
    public static byte[] aesEncrypt(byte[] data, byte[] keyBytes) throws Exception {
        SecretKey key = new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data);
        // prepend IV to result
        byte[] result = new byte[12 + encrypted.length];
        System.arraycopy(iv, 0, result, 0, 12);
        System.arraycopy(encrypted, 0, result, 12, encrypted.length);
        return result;
    }

    public static byte[] aesDecrypt(byte[] data, byte[] keyBytes) throws Exception {
        SecretKey key = new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "AES");
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] cipherText = Arrays.copyOfRange(data, 12, data.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(cipherText);
    }

    // AES key from seed (SHA-256 to get 32 bytes)
    public static byte[] deriveAesKey(byte[] seedBlock) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(seedBlock);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ── COMPRESSION ───────────────────────────────────────────────────────────
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) { gz.write(data); }
        return bos.toByteArray();
    }

    public static byte[] decompress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buf = new byte[4096]; int len;
            while ((len = gz.read(buf)) != -1) bos.write(buf, 0, len);
        }
        return bos.toByteArray();
    }

    public static double compressionRatio(long orig, long comp) {
        if (comp == 0) return 1.0;
        return Math.round(orig * 100.0 / comp) / 100.0;
    }

    // ── SHA-256 ────────────────────────────────────────────────────────────────
    public static String sha256Hash(byte[] data) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    }

    // ── FORMAT ─────────────────────────────────────────────────────────────────
    public static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        // Remove path separators and null bytes
        String sanitized = name.replaceAll("[/\\\\\\x00]", "");
        // Remove leading dots (hidden files / directory traversal)
        sanitized = sanitized.replaceFirst("^\\.+", "");
        // Limit length
        if (sanitized.length() > 255) sanitized = sanitized.substring(0, 255);
        return sanitized.isBlank() ? "unnamed" : sanitized;
    }

    public static String formatFileSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.2f MB", b / (1024.0 * 1024));
    }
}
