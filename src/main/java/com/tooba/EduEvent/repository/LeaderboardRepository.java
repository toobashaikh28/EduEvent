package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {

    // Ranked results for public GET endpoint
    List<Leaderboard> findAllByHackathonIdOrderByRankAsc(Long hackathonId);

    // Check if winners already announced for this hackathon
    boolean existsByHackathonId(Long hackathonId);

    // Aggregate: sum scores per team for a hackathon
    @Query("""
        SELECT s.submission.team.id as teamId, SUM(s.scoreValue) as total
        FROM Score s
        WHERE s.submission.hackathon.id = :hackathonId
        GROUP BY s.submission.team.id
        ORDER BY total DESC
        """)
    List<Object[]> aggregateScoresByTeam(@Param("hackathonId") Long hackathonId);
}