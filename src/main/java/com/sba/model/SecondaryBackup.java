package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Phase 2: Multi-Cloud Simulation
 * Simulates a second independent remote backup server
 */
@Entity
@Table(name = "secondary_backups")
public class SecondaryBackup {

    @Id
    @Column(name = "secondary_id")
    private String secondaryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private CloudFile cloudFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    // Double-XOR: File XOR Seed XOR ClientID_reversed for extra security
    @Lob
    @Column(name = "file_double_xor", columnDefinition = "LONGBLOB")
    private byte[] fileDoubleXor;

    @Column(name = "compressed_size")
    private long compressedSize;

    @Column(name = "is_compressed", nullable = true)
    private Boolean compressed = false;

    @Column(name = "backed_up_at")
    private LocalDateTime backedUpAt = LocalDateTime.now();

    public String getSecondaryId() { return secondaryId; }
    public void setSecondaryId(String v) { this.secondaryId = v; }
    public CloudFile getCloudFile() { return cloudFile; }
    public void setCloudFile(CloudFile v) { this.cloudFile = v; }
    public Client getClient() { return client; }
    public void setClient(Client v) { this.client = v; }
    public byte[] getFileDoubleXor() { return fileDoubleXor; }
    public void setFileDoubleXor(byte[] v) { this.fileDoubleXor = v; }
    public long getCompressedSize() { return compressedSize; }
    public void setCompressedSize(long v) { this.compressedSize = v; }
    public boolean isCompressed() { return compressed != null && compressed; }
    public void setCompressed(Boolean v) { this.compressed = v; }
    public LocalDateTime getBackedUpAt() { return backedUpAt; }
    public void setBackedUpAt(LocalDateTime v) { this.backedUpAt = v; }
}
