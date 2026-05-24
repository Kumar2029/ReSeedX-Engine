package com.sba.repository;

import com.sba.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findByFileIdOrderByVersionNumberDesc(String fileId);
    Optional<FileVersion> findTopByFileIdOrderByVersionNumberDesc(String fileId);
    Optional<FileVersion> findByFileIdAndVersionNumber(String fileId, int versionNumber);
}
