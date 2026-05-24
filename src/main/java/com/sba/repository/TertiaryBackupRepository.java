package com.sba.repository;

import com.sba.model.TertiaryBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TertiaryBackupRepository extends JpaRepository<TertiaryBackup, String> {
    Optional<TertiaryBackup> findByCloudFileFileId(String fileId);
    void deleteByCloudFileFileId(String fileId);
}
