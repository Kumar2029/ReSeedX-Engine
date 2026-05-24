package com.sba.repository;

import com.sba.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByOrgId(String orgId);
    List<TeamMember> findByClientId(String clientId);
    Optional<TeamMember> findByOrgIdAndClientId(String orgId, String clientId);
    void deleteByOrgIdAndClientId(String orgId, String clientId);
}
