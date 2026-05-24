package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cloud_files")
public class CloudFile {

    @Id
    @Column(name = "file_id")
    private String fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "compressed_size")
    private long compressedSize;

    @Column(name = "is_compressed", nullable = true)
    private Boolean compressed = false;

    @Column(name = "is_duplicate", nullable = true)
    private Boolean duplicate = false;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "upload_time_ms")
    private long uploadTimeMs;

    @Column(name = "compression_ratio")
    private double compressionRatio = 1.0;

    @Lob
    @Column(name = "file_data", columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public String getFileId() { return fileId; }
    public void setFileId(String v) { this.fileId = v; }
    public Client getClient() { return client; }
    public void setClient(Client v) { this.client = v; }
    public String getFileName() { return fileName; }
    public void setFileName(String v) { this.fileName = v; }
    public String getFileType() { return fileType; }
    public void setFileType(String v) { this.fileType = v; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long v) { this.fileSize = v; }
    public long getCompressedSize() { return compressedSize; }
    public void setCompressedSize(long v) { this.compressedSize = v; }
    public boolean isCompressed() { return compressed != null && compressed; }
    public void setCompressed(Boolean v) { this.compressed = v; }
    public boolean isDuplicate() { return duplicate != null && duplicate; }
    public void setDuplicate(Boolean v) { this.duplicate = v; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String v) { this.fileHash = v; }
    public long getUploadTimeMs() { return uploadTimeMs; }
    public void setUploadTimeMs(long v) { this.uploadTimeMs = v; }
    public double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(double v) { this.compressionRatio = v; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] v) { this.fileData = v; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime v) { this.uploadedAt = v; }
}
