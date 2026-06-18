package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.TeamMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends MongoRepository<TeamMember, String> {

    long countByTeamIdAndStatus(String teamId, String status);

    List<TeamMember> findAllByTeamIdAndStatus(String teamId, String status);

    List<TeamMember> findByTeamId(String teamId);

    // Checks if a user is the ACCEPTED leader of a specific team
    boolean existsByTeamIdAndUserIdAndRoleAndStatus(String teamId, String userId, String role, String status);

    Optional<TeamMember> findByTeamIdAndUserId(String teamId, String userId);

    // The user's memberships — the service filters these to a hackathon via the team's hackathonId
    List<TeamMember> findByUserId(String userId);

    List<TeamMember> findByUserIdAndStatus(String userId, String status);
}
