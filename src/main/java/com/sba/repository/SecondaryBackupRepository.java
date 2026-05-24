package com.sba.repository;

import com.sba.model.SecondaryBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SecondaryBackupRepository extends JpaRepository<SecondaryBackup, String> {
    Optional<SecondaryBackup> findByCloudFileFileId(String fileId);
    void deleteByCloudFileFileId(String fileId);
}
