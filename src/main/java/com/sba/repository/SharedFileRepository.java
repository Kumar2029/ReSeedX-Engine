package com.sba.repository;

import com.sba.model.SharedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SharedFileRepository extends JpaRepository<SharedFile, Long> {
    List<SharedFile> findByOrgId(String orgId);
    List<SharedFile> findByFileId(String fileId);
    void deleteByFileIdAndOrgId(String fileId, String orgId);
}
