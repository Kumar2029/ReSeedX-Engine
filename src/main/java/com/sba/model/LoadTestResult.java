package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "load_test_results")
public class LoadTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_name")
    private String testName;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes;

    @Column(name = "file_size_label")
    private String fileSizeLabel;

    @Column(name = "xor_time_ms")
    private long xorTimeMs;

    @Column(name = "aes_time_ms")
    private long aesTimeMs;

    @Column(name = "total_time_ms")
    private long totalTimeMs;

    @Column(name = "compression_ratio")
    private double compressionRatio;

    @Column(name = "throughput_kbps")
    private double throughputKbps;

    @Column(name = "verified")
    private boolean verified;

    @Column(name = "thread_count")
    private int threadCount;

    @Column(name = "run_at")
    private LocalDateTime runAt = LocalDateTime.now();

    @Column(name = "client_id")
    private String clientId;

    public Long getId() { return id; }
    public String getTestName() { return testName; }
    public void setTestName(String v) { this.testName = v; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long v) { this.fileSizeBytes = v; }
    public String getFileSizeLabel() { return fileSizeLabel; }
    public void setFileSizeLabel(String v) { this.fileSizeLabel = v; }
    public long getXorTimeMs() { return xorTimeMs; }
    public void setXorTimeMs(long v) { this.xorTimeMs = v; }
    public long getAesTimeMs() { return aesTimeMs; }
    public void setAesTimeMs(long v) { this.aesTimeMs = v; }
    public long getTotalTimeMs() { return totalTimeMs; }
    public void setTotalTimeMs(long v) { this.totalTimeMs = v; }
    public double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(double v) { this.compressionRatio = v; }
    public double getThroughputKbps() { return throughputKbps; }
    public void setThroughputKbps(double v) { this.throughputKbps = v; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean v) { this.verified = v; }
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int v) { this.threadCount = v; }
    public LocalDateTime getRunAt() { return runAt; }
    public void setRunAt(LocalDateTime v) { this.runAt = v; }
    public String getClientId() { return clientId; }
    public void setClientId(String v) { this.clientId = v; }
}
