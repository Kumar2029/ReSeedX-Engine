package com.sba.controller;

import com.sba.model.Client;
import com.sba.repository.ClientRepository;
import com.sba.service.AuditService;
import com.sba.service.SBAService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired private SBAService sbaService;
    @Autowired private ClientRepository clientRepo;
    @Autowired private AuditService auditService;

    private void requireAdmin(HttpServletRequest req) {
        String role = (String) req.getAttribute("role");
        if (!"ADMIN".equals(role)) throw new RuntimeException("Admin access required");
    }

    @GetMapping("/users")
    public ResponseEntity<?> users(HttpServletRequest req) {
        try {
            requireAdmin(req);
            List<Map<String, Object>> users = sbaService.getAllUsers().stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("clientId", c.getClientId()); m.put("username", c.getUsername());
                m.put("role", c.getRole()); m.put("active", c.isActive());
                m.put("createdAt", c.getCreatedAt().toString().replace("T"," ").substring(0,19));
                m.put("lastLogin", c.getLastLogin() != null ? c.getLastLogin().toString().replace("T"," ").substring(0,19) : "Never");
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (Exception e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/users/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable String id, HttpServletRequest req) {
        try { requireAdmin(req); sbaService.toggleUserActive(id); return ResponseEntity.ok(Map.of("message", "Updated")); }
        catch (Exception e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, HttpServletRequest req) {
        try { requireAdmin(req); sbaService.deleteUser(id); return ResponseEntity.ok(Map.of("message", "Deleted")); }
        catch (Exception e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(HttpServletRequest req) {
        try { requireAdmin(req); return ResponseEntity.ok(sbaService.getSystemStats()); }
        catch (Exception e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/audit")
    public ResponseEntity<?> audit(HttpServletRequest req) {
        try { requireAdmin(req); return ResponseEntity.ok(auditService.getRecent()); }
        catch (Exception e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            requireAdmin(req);
            Client c = sbaService.register(body.get("username"), body.get("password"), true);
            return ResponseEntity.ok(Map.of("message", "Admin created", "username", c.getUsername()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
