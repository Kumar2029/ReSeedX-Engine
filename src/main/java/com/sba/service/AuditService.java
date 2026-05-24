package com.sba.service;

import com.sba.model.AuditLog;
import com.sba.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {
    @Autowired private AuditLogRepository repo;

    public void log(String clientId, String username, String action, String fileName, Long fileSize, String details, boolean success, String ip, Long durationMs) {
        AuditLog log = new AuditLog();
        log.setClientId(clientId); log.setUsername(username); log.setAction(action);
        log.setFileName(fileName); log.setFileSize(fileSize); log.setDetails(details);
        log.setSuccess(success); log.setIpAddress(ip); log.setDurationMs(durationMs);
        repo.save(log);
    }

    public List<AuditLog> getByClient(String clientId) { return repo.findByClientIdOrderByCreatedAtDesc(clientId); }
    public List<AuditLog> getRecent() { return repo.findTop50ByOrderByCreatedAtDesc(); }
}
