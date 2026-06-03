package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.LeaderboardResponse;
import java.util.List;

public interface LeaderboardService {
    void upsertQuizScore(Long userId, Long eventId, Double score);
    List<LeaderboardResponse> getEventLeaderboard(Long eventId);
    List<LeaderboardResponse> getHackathonLeaderboard(Long hackathonId);
    List<LeaderboardResponse> getGlobalLeaderboard();
}