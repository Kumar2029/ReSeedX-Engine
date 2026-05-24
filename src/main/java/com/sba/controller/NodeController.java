package com.sba.controller;

import com.sba.service.NodeStatusService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Node Control API
 * GET  /api/nodes/status          → get status of all nodes
 * POST /api/nodes/crash?node=A    → crash node A or B
 * POST /api/nodes/restore?node=A  → restore node A or B
 */
@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    @Autowired private NodeStatusService nodeStatusService;

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
            "nodes", nodeStatusService.getAllStatuses(),
            "nodeA", nodeStatusService.getStatus("A").name(),
            "nodeB", nodeStatusService.getStatus("B").name()
        ));
    }

    @PostMapping("/crash")
    public ResponseEntity<?> crash(@RequestParam String node, HttpServletRequest req) {
        String role = (String) req.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        String n = node.toUpperCase();
        if (!n.equals("A") && !n.equals("B") && !n.equals("C"))
            return ResponseEntity.badRequest().body(Map.of("error", "Node must be A, B or C"));
        nodeStatusService.crash(n);
        return ResponseEntity.ok(Map.of(
            "message", "Node " + n + " crashed",
            "node", n,
            "status", "FAILED"
        ));
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestParam String node, HttpServletRequest req) {
        String role = (String) req.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        String n = node.toUpperCase();
        if (!n.equals("A") && !n.equals("B") && !n.equals("C"))
            return ResponseEntity.badRequest().body(Map.of("error", "Node must be A, B or C"));
        nodeStatusService.restore(n);
        return ResponseEntity.ok(Map.of(
            "message", "Node " + n + " restored",
            "node", n,
            "status", "ACTIVE"
        ));
    }
}
