package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.LeaderboardResponse;
import com.tooba.EduEvent.entity.Leaderboard;
import com.tooba.EduEvent.repository.LeaderboardRepository;
import com.tooba.EduEvent.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardServiceImpl implements LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    @Override
    @Transactional
    public void upsertQuizScore(Long userId, Long eventId, Double score) {
        leaderboardRepository.upsertQuizScore(userId, eventId, score);
        log.info("Merged leaderboard score {} for user {} in event {}", score, userId, eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getEventLeaderboard(Long eventId) {
        List<Leaderboard> results = leaderboardRepository.findEventLeaderboard(eventId);
        return mapToResponse(results, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getHackathonLeaderboard(Long hackathonId) {
        List<Leaderboard> results = leaderboardRepository.findAllByHackathonIdOrderByRankAsc(hackathonId);
        return mapToResponse(results, false); 
    }

    // 🔥 CACHED METHOD: Prevents slamming the database with heavy SUM() aggregations
    @Override
    @Cacheable("global-leaderboard")
    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getGlobalLeaderboard() {
        log.info("Fetching Global Leaderboard from Database (Cache Miss)");
        List<Object[]> rawResults = leaderboardRepository.findGlobalLeaderboard();
        
        List<LeaderboardResponse> response = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rawResults) {
            LeaderboardResponse dto = new LeaderboardResponse();
            dto.setRank(rank++);
            dto.setTeamOrUserName((String) row[0]);
            dto.setTotalPoints(((Number) row[1]).intValue()); 
            response.add(dto);
        }
        return response;
    }

    // Helper Mapper
    private List<LeaderboardResponse> mapToResponse(List<Leaderboard> boards, boolean isUser) {
        List<LeaderboardResponse> response = new ArrayList<>();
        int rank = 1;
        for (Leaderboard b : boards) {
            LeaderboardResponse dto = new LeaderboardResponse();
            dto.setRank(rank++);
            
            // Handle Name & Score (User vs Team)
            if (isUser) {
                dto.setTeamOrUserName(b.getUser() != null ? b.getUser().getName() : "Unknown User");
                dto.setTotalPoints(b.getScore() != null ? b.getScore().intValue() : 0);
            } else {
                dto.setTeamOrUserName(b.getTeam() != null ? b.getTeam().getName() : "Unknown Team");
                dto.setTotalPoints(b.getTotalScore() != null ? b.getTotalScore().intValue() : 0);
            }
            
            response.add(dto);
        }
        return response;
    }
}