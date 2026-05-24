package com.sba.controller;

import com.sba.model.*;
import com.sba.repository.*;
import com.sba.util.SBAUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/team")
public class TeamController {

    @Autowired private OrganizationRepository orgRepo;
    @Autowired private TeamMemberRepository memberRepo;
    @Autowired private SharedFileRepository sharedFileRepo;
    @Autowired private ClientRepository clientRepo;
    @Autowired private CloudFileRepository cloudFileRepo;

    private String getClientId(HttpServletRequest req) {
        String id = (String) req.getAttribute("clientId");
        if (id == null) throw new RuntimeException("Unauthorized");
        return id;
    }

    // Create organization
    @PostMapping("/org")
    public ResponseEntity<?> createOrg(@RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            String clientId = getClientId(req);
            String name = body.get("name");
            if (name == null || name.trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Name required"));
            Organization org = new Organization();
            org.setOrgId(UUID.randomUUID().toString().substring(0, 8));
            org.setOrgName(name.trim());
            org.setOwnerId(clientId);
            orgRepo.save(org);
            // Add owner as member
            TeamMember tm = new TeamMember();
            tm.setOrgId(org.getOrgId());
            tm.setClientId(clientId);
            tm.setRole("OWNER");
            memberRepo.save(tm);
            return ResponseEntity.ok(Map.of("message", "Organization created", "orgId", org.getOrgId(), "orgName", org.getOrgName()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // Get my organizations
    @GetMapping("/orgs")
    public ResponseEntity<?> myOrgs(HttpServletRequest req) {
        try {
            String clientId = getClientId(req);
            List<TeamMember> memberships = memberRepo.findByClientId(clientId);
            List<Map<String, Object>> orgs = memberships.stream().map(m -> {
                Organization org = orgRepo.findById(m.getOrgId()).orElse(null);
                if (org == null) return null;
                Map<String, Object> r = new HashMap<>();
                r.put("orgId", org.getOrgId());
                r.put("orgName", org.getOrgName());
                r.put("role", m.getRole());
                r.put("memberCount", memberRepo.findByOrgId(org.getOrgId()).size());
                return r;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            return ResponseEntity.ok(orgs);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // Add member to org (by username)
    @PostMapping("/org/{orgId}/members")
    public ResponseEntity<?> addMember(@PathVariable String orgId, @RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            String clientId = getClientId(req);
            // Check caller is owner/admin of org
            TeamMember caller = memberRepo.findByOrgIdAndClientId(orgId, clientId).orElseThrow(() -> new RuntimeException("Not a member"));
            if (!caller.getRole().equals("OWNER") && !caller.getRole().equals("ADMIN"))
                throw new RuntimeException("Only owner/admin can add members");
            String username = body.get("username");
            Client target = clientRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User '" + username + "' not found"));
            if (memberRepo.findByOrgIdAndClientId(orgId, target.getClientId()).isPresent())
                return ResponseEntity.badRequest().body(Map.of("error", "Already a member"));
            TeamMember tm = new TeamMember();
            tm.setOrgId(orgId);
            tm.setClientId(target.getClientId());
            tm.setRole("MEMBER");
            memberRepo.save(tm);
            return ResponseEntity.ok(Map.of("message", username + " added to team"));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // Get members of org
    @GetMapping("/org/{orgId}/members")
    public ResponseEntity<?> getMembers(@PathVariable String orgId, HttpServletRequest req) {
        try {
            getClientId(req);
            List<Map<String, Object>> members = memberRepo.findByOrgId(orgId).stream().map(m -> {
                Client c = clientRepo.findById(m.getClientId()).orElse(null);
                Map<String, Object> r = new HashMap<>();
                r.put("clientId", m.getClientId());
                r.put("username", c != null ? c.getUsername() : "Unknown");
                r.put("role", m.getRole());
                r.put("joinedAt", m.getJoinedAt().toString().replace("T", " ").substring(0, 19));
                return r;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(members);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // Remove member
    @Transactional
    @DeleteMapping("/org/{orgId}/members/{targetId}")
    public ResponseEntity<?> removeMember(@PathVariable String orgId, @PathVariable String targetId, HttpServletRequest req) {
        try {
            String clientId = getClientId(req);
            TeamMember caller = memberRepo.findByOrgIdAndClientId(orgId, clientId).orElseThrow(() -> new RuntimeException("Not a member"));
            if (!caller.getRole().equals("OWNER")) throw new RuntimeException("Only owner can remove members");
            memberRepo.deleteByOrgIdAndClientId(orgId, targetId);
            return ResponseEntity.ok(Map.of("message", "Member removed"));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // Share file with org
    @PostMapping("/org/{orgId}/share/{fileId}")
    public ResponseEntity<?> shareFile(@PathVariable String orgId, @PathVariable String fileId, HttpServletRequest req) {
        try {
            String clientId = getClientId(req);
            memberRepo.findByOrgIdAndClientId(orgId, clientId).orElseThrow(() -> new RuntimeException("Not a member"));
            SharedFile sf = new SharedFile();
            sf.setFileId(fileId);
            sf.setOrgId(orgId);
            sf.setSharedBy(clientId);
            sharedFileRepo.save(sf);
            return ResponseEntity.ok(Map.of("message", "File shared with team"));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // Get shared files for org
    @GetMapping("/org/{orgId}/files")
    public ResponseEntity<?> sharedFiles(@PathVariable String orgId, HttpServletRequest req) {
        try {
            String clientId = getClientId(req);
            memberRepo.findByOrgIdAndClientId(orgId, clientId).orElseThrow(() -> new RuntimeException("Not a member"));
            List<Map<String, Object>> files = sharedFileRepo.findByOrgId(orgId).stream().map(sf -> {
                CloudFile cf = cloudFileRepo.findById(sf.getFileId()).orElse(null);
                if (cf == null) return null;
                Client sharer = clientRepo.findById(sf.getSharedBy()).orElse(null);
                Map<String, Object> r = new HashMap<>();
                r.put("fileId", cf.getFileId());
                r.put("fileName", cf.getFileName());
                r.put("fileSize", SBAUtils.formatFileSize(cf.getFileSize()));
                r.put("sharedBy", sharer != null ? sharer.getUsername() : "Unknown");
                r.put("sharedAt", sf.getSharedAt().toString().replace("T", " ").substring(0, 19));
                return r;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            return ResponseEntity.ok(files);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
