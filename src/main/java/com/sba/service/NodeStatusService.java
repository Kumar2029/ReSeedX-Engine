package com.sba.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Node Status Service.
 * Tracks whether Node A (Primary / AES+XOR) and Node B (Secondary / DoubleXOR)
 * are ACTIVE or FAILED. Used by SBAService to simulate fault-tolerance.
 */
@Service
public class NodeStatusService {

    public enum NodeStatus { ACTIVE, FAILED }

    private final Map<String, NodeStatus> nodes = new ConcurrentHashMap<>();

    public NodeStatusService() {
        nodes.put("A", NodeStatus.ACTIVE);
        nodes.put("B", NodeStatus.ACTIVE);
        nodes.put("C", NodeStatus.ACTIVE);
    }

    public NodeStatus getStatus(String node) {
        return nodes.getOrDefault(node.toUpperCase(), NodeStatus.ACTIVE);
    }

    public boolean isActive(String node) {
        return getStatus(node) == NodeStatus.ACTIVE;
    }

    public void crash(String node) {
        nodes.put(node.toUpperCase(), NodeStatus.FAILED);
    }

    public void restore(String node) {
        nodes.put(node.toUpperCase(), NodeStatus.ACTIVE);
    }

    public Map<String, String> getAllStatuses() {
        Map<String, String> result = new ConcurrentHashMap<>();
        nodes.forEach((k, v) -> result.put(k, v.name()));
        return result;
    }
}
