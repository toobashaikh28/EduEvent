package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.LeaderboardResponse;
import java.util.List;

public interface LeaderboardService {
    void upsertQuizScore(String userId, String eventId, Double score);
    List<LeaderboardResponse> getEventLeaderboard(String eventId);
    List<LeaderboardResponse> getHackathonLeaderboard(String hackathonId);
    List<LeaderboardResponse> getGlobalLeaderboard();
}