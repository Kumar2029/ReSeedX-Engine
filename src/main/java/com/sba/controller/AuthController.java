package com.sba.controller;

import com.sba.model.Client;
import com.sba.repository.ClientRepository;
import com.sba.service.AuditService;
import com.sba.service.SBAService;
import com.sba.util.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "JWT-based authentication")
public class AuthController {

    @Autowired private SBAService sbaService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AuditService audit;
    @Autowired private ClientRepository clientRepo;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            String u = body.get("username"), p = body.get("password");
            if (u == null || u.trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Username required"));
            if (p == null || p.length() < 8) return ResponseEntity.badRequest().body(Map.of("error", "Password min 8 chars"));
            if (!p.matches(".*[A-Z].*") || !p.matches(".*[0-9].*")) return ResponseEntity.badRequest().body(Map.of("error", "Password needs uppercase + number"));
            Client c = sbaService.register(u.trim(), p);
            audit.log(c.getClientId(), c.getUsername(), "REGISTER", null, null, "New account [" + c.getRole() + "]", true, req.getRemoteAddr(), null);
            return ResponseEntity.ok(Map.of("message", "Account created!", "username", c.getUsername(), "role", c.getRole()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            long start = System.currentTimeMillis();
            return sbaService.login(body.get("username"), body.get("password")).map(c -> {
                String token = jwtUtil.generateToken(c.getClientId(), c.getUsername(), c.getRole());
                audit.log(c.getClientId(), c.getUsername(), "LOGIN", null, null, "JWT issued", true, req.getRemoteAddr(), System.currentTimeMillis() - start);
                return ResponseEntity.ok(Map.of("token", token, "username", c.getUsername(), "role", c.getRole(), "message", "Login successful"));
            }).orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials or account disabled")));
        } catch (Exception e) { return ResponseEntity.status(500).body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        String clientId = (String) req.getAttribute("clientId");
        if (clientId == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return clientRepo.findById(clientId).map(c ->
            ResponseEntity.ok(Map.of("username", c.getUsername(), "clientId", c.getClientId(), "role", c.getRole()))
        ).orElse(ResponseEntity.status(401).body(Map.of("error", "Not found")));
    }
}
