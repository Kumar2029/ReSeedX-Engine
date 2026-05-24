package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_blocks")
public class AuditBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "block_index")
    private long blockIndex;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(name = "block_hash", length = 64)
    private String blockHash;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "action")
    private String action;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    @Column(name = "nonce")
    private long nonce;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    public Long getId() { return id; }
    public long getBlockIndex() { return blockIndex; }
    public void setBlockIndex(long v) { this.blockIndex = v; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String v) { this.previousHash = v; }
    public String getBlockHash() { return blockHash; }
    public void setBlockHash(String v) { this.blockHash = v; }
    public String getClientId() { return clientId; }
    public void setClientId(String v) { this.clientId = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getData() { return data; }
    public void setData(String v) { this.data = v; }
    public long getNonce() { return nonce; }
    public void setNonce(long v) { this.nonce = v; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime v) { this.timestamp = v; }
}
