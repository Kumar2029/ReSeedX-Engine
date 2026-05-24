package com.sba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_members")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "role")
    private String role = "MEMBER"; // OWNER, ADMIN, MEMBER

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String v) { this.orgId = v; }
    public String getClientId() { return clientId; }
    public void setClientId(String v) { this.clientId = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
