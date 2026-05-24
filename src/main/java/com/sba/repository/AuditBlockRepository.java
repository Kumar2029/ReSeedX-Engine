package com.sba.repository;

import com.sba.model.AuditBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AuditBlockRepository extends JpaRepository<AuditBlock, Long> {
    Optional<AuditBlock> findTopByOrderByBlockIndexDesc();
    List<AuditBlock> findAllByOrderByBlockIndexAsc();
    List<AuditBlock> findByClientIdOrderByBlockIndexDesc(String clientId);
    List<AuditBlock> findByClientIdOrderByBlockIndexAsc(String clientId);
}
