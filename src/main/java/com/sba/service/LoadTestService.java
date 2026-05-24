package com.sba.service;

import com.sba.model.LoadTestResult;
import com.sba.repository.LoadTestResultRepository;
import com.sba.util.SBAUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
public class LoadTestService {

    @Autowired private LoadTestResultRepository repo;

    private static final Object[][] TEST_SIZES = {
        {1,    "1 KB"},   {10,   "10 KB"},  {50,   "50 KB"},
        {100,  "100 KB"}, {500,  "500 KB"}, {1024, "1 MB"},
        {2048, "2 MB"},   {5120, "5 MB"},   {10240,"10 MB"}
    };

    @Transactional
    public List<LoadTestResult> runLoadTest(byte[] seedBlock, String clientId) throws Exception {
        repo.deleteByClientId(clientId);
        List<LoadTestResult> results = new ArrayList<>();
        byte[] aesKey = SBAUtils.deriveAesKey(seedBlock);

        for (Object[] sizeInfo : TEST_SIZES) {
            int sizeKB = (int) sizeInfo[0];
            String label = (String) sizeInfo[1];
            byte[] data = new byte[sizeKB * 1024];
            new SecureRandom().nextBytes(data);

            // Compress
            byte[] compressed; double ratio = 1.0;
            try { compressed = SBAUtils.compress(data);
                if (compressed.length < data.length * 0.95) { ratio = SBAUtils.compressionRatio(data.length, compressed.length); }
                else compressed = data;
            } catch (Exception e) { compressed = data; }

            // XOR multi-threaded
            long xorStart = System.currentTimeMillis();
            byte[] xord = SBAUtils.xorMultiThreaded(compressed, seedBlock);
            long xorTime = System.currentTimeMillis() - xorStart;

            // AES-256
            long aesStart = System.currentTimeMillis();
            byte[] aesEnc = SBAUtils.aesEncrypt(xord, aesKey);
            byte[] aesDec = SBAUtils.aesDecrypt(aesEnc, aesKey);
            long aesTime = System.currentTimeMillis() - aesStart;

            // Verify
            byte[] recovered = SBAUtils.xorWithSeed(aesDec, seedBlock);
            try { if (ratio > 1.0) recovered = SBAUtils.decompress(recovered); } catch (Exception ignored) {}
            boolean verified = SBAUtils.sha256Hash(data).equals(SBAUtils.sha256Hash(recovered));

            long total = xorTime + aesTime;
            double throughput = total > 0 ? Math.round(sizeKB * 1000.0 / total * 10) / 10.0 : sizeKB * 100.0;

            LoadTestResult r = new LoadTestResult();
            r.setTestName("SBA-P3-" + label);
            r.setFileSizeBytes((long) sizeKB * 1024);
            r.setFileSizeLabel(label);
            r.setXorTimeMs(xorTime);
            r.setAesTimeMs(aesTime);
            r.setTotalTimeMs(total);
            r.setCompressionRatio(ratio);
            r.setThroughputKbps(throughput);
            r.setVerified(verified);
            r.setThreadCount(Math.min(4, (int) Math.ceil(sizeKB * 1024.0 / (1024 * 1024))));
            r.setClientId(clientId);
            repo.save(r); results.add(r);
        }
        return results;
    }

    public List<LoadTestResult> getLatest(String clientId) { return repo.findTop20ByClientIdOrderByRunAtDesc(clientId); }
}
