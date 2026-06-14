package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {

    // --- YOUR EXISTING METHODS ---
    List<Leaderboard> findAllByHackathonIdOrderByRankAsc(Long hackathonId);
    boolean existsByHackathonId(Long hackathonId);

    @Query("""
        SELECT s.submission.team.id as teamId, SUM(s.scoreValue) as total
        FROM Score s
        WHERE s.submission.hackathon.id = :hackathonId
        GROUP BY s.submission.team.id
        ORDER BY total DESC
        """)
    List<Object[]> aggregateScoresByTeam(@Param("hackathonId") Long hackathonId);


    // --- NEW METHODS FOR QUIZZES & GLOBAL LEADERBOARD ---

    // 1. SQL Server MERGE (Upsert): Keeps the highest score if the user takes the quiz multiple times
    @Modifying
    @Query(value = "MERGE INTO leaderboard AS target " +
                   "USING (SELECT :userId AS user_id, :eventId AS event_id, :score AS score) AS source " +
                   "ON target.user_id = source.user_id AND target.event_id = source.event_id " +
                   "WHEN MATCHED THEN " +
                   "    UPDATE SET target.score = CASE WHEN source.score > target.score THEN source.score ELSE target.score END " +
                   "WHEN NOT MATCHED THEN " +
                   "    INSERT (user_id, event_id, score, rank) VALUES (source.user_id, source.event_id, source.score, 0);", 
           nativeQuery = true)
    void upsertQuizScore(@Param("userId") Long userId, @Param("eventId") Long eventId, @Param("score") Double score);

    // 2. Event Leaderboard (Quizzes)
    @Query("SELECT l FROM Leaderboard l WHERE l.event.id = :eventId AND l.user IS NOT NULL ORDER BY l.score DESC")
    List<Leaderboard> findEventLeaderboard(@Param("eventId") Long eventId);

    // 3. Global Leaderboard (Sum of all quiz scores per user)
    @Query("SELECT l.user.name, SUM(l.score) as totalScore " +
           "FROM Leaderboard l WHERE l.user IS NOT NULL " +
           "GROUP BY l.user.id, l.user.name ORDER BY totalScore DESC")
    List<Object[]> findGlobalLeaderboard();
}