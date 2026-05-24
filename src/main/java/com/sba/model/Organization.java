package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @Column(name = "org_id")
    private String orgId;

    @Column(name = "org_name", nullable = false)
    private String orgName;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getOrgId() { return orgId; }
    public void setOrgId(String v) { this.orgId = v; }
    public String getOrgName() { return orgName; }
    public void setOrgName(String v) { this.orgName = v; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String v) { this.ownerId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
