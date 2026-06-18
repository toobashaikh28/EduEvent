package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Leaderboard;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LeaderboardRepository extends MongoRepository<Leaderboard, String> {

    // Hackathon team rankings
    List<Leaderboard> findAllByHackathonIdOrderByRankAsc(String hackathonId);
    boolean existsByHackathonId(String hackathonId);

    // Event (quiz) leaderboard — entries that belong to a user, highest score first
    List<Leaderboard> findByEventIdAndUserIdNotNullOrderByScoreDesc(String eventId);

    // Used by the quiz-score upsert logic in the service
    Optional<Leaderboard> findByEventIdAndUserId(String eventId, String userId);

    // All user (quiz) leaderboard rows — global leaderboard is summed in the service
    List<Leaderboard> findByUserIdNotNull();
}
