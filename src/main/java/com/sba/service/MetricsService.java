package com.sba.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private final ConcurrentLinkedDeque<MetricEntry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicLong totalOps = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private static final int MAX_ENTRIES = 1000;

    public void record(String operation, long bytes, long durationMs) {
        entries.addLast(new MetricEntry(operation, bytes, durationMs, System.currentTimeMillis()));
        totalOps.incrementAndGet();
        totalBytes.addAndGet(bytes);
        while (entries.size() > MAX_ENTRIES) entries.pollFirst();
    }

    public Map<String, Object> getMetrics() {
        List<MetricEntry> snapshot = new ArrayList<>(entries);
        Map<String, Object> m = new HashMap<>();
        m.put("totalOperations", totalOps.get());
        m.put("totalBytesProcessed", totalBytes.get());
        m.put("totalBytesFormatted", formatSize(totalBytes.get()));

        // Throughput (last 60s)
        long now = System.currentTimeMillis();
        long cutoff = now - 60000;
        List<MetricEntry> recent = snapshot.stream().filter(e -> e.timestamp > cutoff).toList();
        long recentBytes = recent.stream().mapToLong(e -> e.bytes).sum();
        long recentMs = recent.stream().mapToLong(e -> e.durationMs).sum();
        m.put("throughputMBps", recentMs > 0 ? Math.round(recentBytes * 1000.0 / recentMs / 1048576 * 100) / 100.0 : 0);
        m.put("opsPerSecond", recent.size() > 0 ? Math.round(recent.size() * 1000.0 / 60000 * 100) / 100.0 : 0);

        // Latency percentiles
        long[] latencies = snapshot.stream().mapToLong(e -> e.durationMs).sorted().toArray();
        if (latencies.length > 0) {
            m.put("p50Ms", latencies[(int)(latencies.length * 0.5)]);
            m.put("p95Ms", latencies[Math.min((int)(latencies.length * 0.95), latencies.length - 1)]);
            m.put("p99Ms", latencies[Math.min((int)(latencies.length * 0.99), latencies.length - 1)]);
            m.put("avgMs", Math.round(Arrays.stream(latencies).average().orElse(0)));
            m.put("minMs", latencies[0]);
            m.put("maxMs", latencies[latencies.length - 1]);
        }

        // Per-operation breakdown
        Map<String, Map<String, Object>> byOp = new HashMap<>();
        for (MetricEntry e : snapshot) {
            byOp.computeIfAbsent(e.operation, k -> {
                Map<String, Object> o = new HashMap<>();
                o.put("count", 0L); o.put("totalMs", 0L); o.put("totalBytes", 0L);
                return o;
            });
            Map<String, Object> o = byOp.get(e.operation);
            o.put("count", (Long)o.get("count") + 1);
            o.put("totalMs", (Long)o.get("totalMs") + e.durationMs);
            o.put("totalBytes", (Long)o.get("totalBytes") + e.bytes);
        }
        byOp.forEach((k, v) -> {
            long count = (Long)v.get("count");
            long totalMs = (Long)v.get("totalMs");
            v.put("avgMs", count > 0 ? totalMs / count : 0);
        });
        m.put("byOperation", byOp);
        m.put("recentEntries", recent.size());
        return m;
    }

    private String formatSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1048576) return String.format("%.1f KB", b / 1024.0);
        if (b < 1073741824) return String.format("%.2f MB", b / 1048576.0);
        return String.format("%.2f GB", b / 1073741824.0);
    }

    private record MetricEntry(String operation, long bytes, long durationMs, long timestamp) {}
}
