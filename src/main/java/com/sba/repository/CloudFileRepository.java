package com.sba.repository;

import com.sba.model.Client;
import com.sba.model.CloudFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CloudFileRepository extends JpaRepository<CloudFile, String> {
    List<CloudFile> findByClientOrderByUploadedAtDesc(Client client);
    Optional<CloudFile> findByClientAndFileHash(Client client, String fileHash);
    List<CloudFile> findAll();
}
