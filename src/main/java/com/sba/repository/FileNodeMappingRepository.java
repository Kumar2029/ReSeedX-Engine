package com.sba.repository;

import com.sba.model.FileNodeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileNodeMappingRepository extends JpaRepository<FileNodeMapping, Long> {
    List<FileNodeMapping> findByFileId(String fileId);
    Optional<FileNodeMapping> findByFileIdAndNodeId(String fileId, int nodeId);
    void deleteByFileId(String fileId);
}
