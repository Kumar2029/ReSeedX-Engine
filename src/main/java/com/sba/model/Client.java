package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @Column(name = "client_id")
    private String clientId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Lob
    @Column(name = "seed_block", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] seedBlock;

    @Column(name = "role", nullable = false)
    private String role = "USER"; // USER or ADMIN

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getClientId() { return clientId; }
    public void setClientId(String v) { this.clientId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public byte[] getSeedBlock() { return seedBlock; }
    public void setSeedBlock(byte[] v) { this.seedBlock = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime v) { this.lastLogin = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
