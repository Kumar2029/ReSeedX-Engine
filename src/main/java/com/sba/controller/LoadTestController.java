package com.sba.controller;

import com.sba.model.Client;
import com.sba.repository.ClientRepository;
import com.sba.service.LoadTestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/loadtest")
public class LoadTestController {
    @Autowired private LoadTestService loadTestService;
    @Autowired private ClientRepository clientRepo;

    @PostMapping("/run")
    public ResponseEntity<?> run(HttpServletRequest req) {
        try {
            String clientId = (String) req.getAttribute("clientId");
            if (clientId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            Client c = clientRepo.findById(clientId).orElseThrow(() -> new RuntimeException("Not found"));
            return ResponseEntity.ok(loadTestService.runLoadTest(c.getSeedBlock(), c.getClientId()));
        } catch (Exception e) { return ResponseEntity.status(500).body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/results")
    public ResponseEntity<?> results(HttpServletRequest req) {
        try {
            String clientId = (String) req.getAttribute("clientId");
            if (clientId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            return ResponseEntity.ok(loadTestService.getLatest(clientId));
        } catch (Exception e) { return ResponseEntity.status(500).body(Map.of("error", e.getMessage())); }
    }
}
