package com.sba.repository;

import com.sba.model.BackupFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BackupFileRepository extends JpaRepository<BackupFile, String> {
    Optional<BackupFile> findByCloudFileFileId(String fileId);
    void deleteByCloudFileFileId(String fileId);
}
