package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "backup_files")
public class BackupFile {

    @Id @Column(name = "backup_id") private String backupId;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "file_id") private CloudFile cloudFile;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id") private Client client;

    @Lob @Column(name = "file_dash", columnDefinition = "LONGBLOB")
    private byte[] fileDash;            // Remote Server 1: AES(XOR(File, Seed))

    @Column(name = "backed_up_at") private LocalDateTime backedUpAt = LocalDateTime.now();

    public String getBackupId() { return backupId; }
    public void setBackupId(String v) { this.backupId = v; }
    public CloudFile getCloudFile() { return cloudFile; }
    public void setCloudFile(CloudFile v) { this.cloudFile = v; }
    public Client getClient() { return client; }
    public void setClient(Client v) { this.client = v; }
    public byte[] getFileDash() { return fileDash; }
    public void setFileDash(byte[] v) { this.fileDash = v; }
    public LocalDateTime getBackedUpAt() { return backedUpAt; }
    public void setBackedUpAt(LocalDateTime v) { this.backedUpAt = v; }
}
