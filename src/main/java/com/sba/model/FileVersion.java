package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_versions")
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id")
    private String fileId;

    @Column(name = "version_number")
    private int versionNumber;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "file_size")
    private long fileSize;

    @Lob
    @Column(name = "file_data", columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "change_summary")
    private String changeSummary;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getFileId() { return fileId; }
    public void setFileId(String v) { this.fileId = v; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int v) { this.versionNumber = v; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String v) { this.fileHash = v; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long v) { this.fileSize = v; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] v) { this.fileData = v; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String v) { this.changeSummary = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
