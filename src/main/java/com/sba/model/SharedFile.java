package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_files")
public class SharedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "shared_by", nullable = false)
    private String sharedBy;

    @Column(name = "shared_at")
    private LocalDateTime sharedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getFileId() { return fileId; }
    public void setFileId(String v) { this.fileId = v; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String v) { this.orgId = v; }
    public String getSharedBy() { return sharedBy; }
    public void setSharedBy(String v) { this.sharedBy = v; }
    public LocalDateTime getSharedAt() { return sharedAt; }
}
