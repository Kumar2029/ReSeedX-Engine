package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_node_mapping")
public class FileNodeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "node_id", nullable = false)
    private int nodeId; // 1 = Primary (AES+XOR), 2 = Secondary (DoubleXOR)

    @Column(name = "node_label")
    private String nodeLabel; // "Node A — Primary" or "Node B — Secondary"

    @Column(name = "type")
    private String type; // MAIN or BACKUP

    @Column(name = "file_path")
    private String filePath; // visual: e.g. storage/node1/filename.enc

    @Column(name = "is_deleted")
    private boolean deleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Getters & Setters
    public Long getId() { return id; }
    public String getFileId() { return fileId; }
    public void setFileId(String v) { this.fileId = v; }
    public int getNodeId() { return nodeId; }
    public void setNodeId(int v) { this.nodeId = v; }
    public String getNodeLabel() { return nodeLabel; }
    public void setNodeLabel(String v) { this.nodeLabel = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String v) { this.filePath = v; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean v) { this.deleted = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime v) { this.deletedAt = v; }
}
