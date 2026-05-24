package com.sba.repository;

import com.sba.model.LoadTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoadTestResultRepository extends JpaRepository<LoadTestResult, Long> {
    List<LoadTestResult> findTop20ByOrderByRunAtDesc();
    List<LoadTestResult> findTop20ByClientIdOrderByRunAtDesc(String clientId);
    void deleteByClientId(String clientId);
}
