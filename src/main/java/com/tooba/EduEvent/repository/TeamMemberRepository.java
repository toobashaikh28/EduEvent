package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Team;      
import com.tooba.EduEvent.entity.TeamMember; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm WHERE tm.user.id = :userId AND tm.team.hackathon.id = :hackathonId")
    boolean existsByUserIdAndHackathonId(@Param("userId") Long userId, @Param("hackathonId") Long hackathonId);

    long countByTeamIdAndStatus(Long teamId, String status);

    List<TeamMember> findAllByTeamIdAndStatus(Long teamId, String status);

    // Checks if a user is the APPROVED/ACCEPTED leader of a specific team (Matches your entity property names beautifully!)
    boolean existsByTeamIdAndUserIdAndRoleAndStatus(Long teamId, Long userId, String role, String status);

    // Mapped perfectly to your nested entities: user.id -> team.hackathon.id -> status
    @Query("SELECT tm.team FROM TeamMember tm WHERE tm.user.id = :userId AND tm.team.hackathon.id = :hackathonId AND tm.status = 'ACCEPTED'")
    Optional<Team> findTeamByUserIdAndHackathonId(@Param("userId") Long userId, @Param("hackathonId") Long hackathonId);

    // Add this to find the user's team
    List<TeamMember> findByUserId(Long userId);
}