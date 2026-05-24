package com.sba.repository;

import com.sba.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, String> {
    List<Organization> findByOwnerId(String ownerId);
}
