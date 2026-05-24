package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Phase 3 Extension: Node C — Third independent backup server
 * Strategy: Triple XOR (File XOR Seed XOR ClientID XOR reversed_seed)
 */
@Entity
@Table(name = "tertiary_backups")
public class TertiaryBackup {

    @Id
    @Column(name = "tertiary_id")
    private String tertiaryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private CloudFile cloudFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    // Triple-XOR: File XOR Seed XOR ClientID XOR reversed(Seed)
    @Lob
    @Column(name = "file_triple_xor", columnDefinition = "LONGBLOB")
    private byte[] fileTripleXor;

    @Column(name = "compressed_size")
    private long compressedSize;

    @Column(name = "is_compressed", nullable = true)
    private Boolean compressed = false;

    @Column(name = "backed_up_at")
    private LocalDateTime backedUpAt = LocalDateTime.now();

    public String getTertiaryId() { return tertiaryId; }
    public void setTertiaryId(String v) { this.tertiaryId = v; }
    public CloudFile getCloudFile() { return cloudFile; }
    public void setCloudFile(CloudFile v) { this.cloudFile = v; }
    public Client getClient() { return client; }
    public void setClient(Client v) { this.client = v; }
    public byte[] getFileTripleXor() { return fileTripleXor; }
    public void setFileTripleXor(byte[] v) { this.fileTripleXor = v; }
    public long getCompressedSize() { return compressedSize; }
    public void setCompressedSize(long v) { this.compressedSize = v; }
    public boolean isCompressed() { return compressed != null && compressed; }
    public void setCompressed(Boolean v) { this.compressed = v; }
    public LocalDateTime getBackedUpAt() { return backedUpAt; }
    public void setBackedUpAt(LocalDateTime v) { this.backedUpAt = v; }
}
