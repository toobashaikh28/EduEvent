package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.LeaderboardResponse;
import com.tooba.EduEvent.entity.Leaderboard;
import com.tooba.EduEvent.entity.Score;
import com.tooba.EduEvent.entity.Submission;
import com.tooba.EduEvent.entity.Team;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.LeaderboardRepository;
import com.tooba.EduEvent.repository.ScoreRepository;
import com.tooba.EduEvent.repository.SubmissionRepository;
import com.tooba.EduEvent.repository.TeamRepository;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardServiceImpl implements LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;

    @Override
    public void upsertQuizScore(String userId, String eventId, Double score) {
        // Keep the highest score if the user takes the quiz multiple times (Mongo upsert)
        Leaderboard row = leaderboardRepository.findByEventIdAndUserId(eventId, userId)
                .orElse(null);
        if (row == null) {
            row = Leaderboard.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .score(score)
                    .rank(0)
                    .build();
        } else if (row.getScore() == null || score > row.getScore()) {
            row.setScore(score);
        }
        leaderboardRepository.save(row);
        log.info("Upserted leaderboard score {} for user {} in event {}", score, userId, eventId);
    }

    @Override
    public List<LeaderboardResponse> getEventLeaderboard(String eventId) {
        List<Leaderboard> results = leaderboardRepository.findByEventIdAndUserIdNotNullOrderByScoreDesc(eventId);
        return mapToResponse(results, true);
    }

    @Override
    public List<LeaderboardResponse> getHackathonLeaderboard(String hackathonId) {
        List<Leaderboard> results = leaderboardRepository.findAllByHackathonIdOrderByRankAsc(hackathonId);
        if (!results.isEmpty()) {
            return mapToResponse(results, false);
        }
        // Winners haven't been officially announced yet (no rows persisted in the
        // leaderboard collection) — fall back to LIVE standings computed directly
        // from judge scores, so participants can watch rankings update in real
        // time instead of seeing a blank leaderboard until an admin announces.
        return computeLiveHackathonStandings(hackathonId);
    }

    private List<LeaderboardResponse> computeLiveHackathonStandings(String hackathonId) {
        List<Submission> subs = submissionRepository.findAllByHackathonId(hackathonId);
        Map<String, String> submissionToTeam = new LinkedHashMap<>();
        for (Submission s : subs) submissionToTeam.put(s.getId(), s.getTeamId());
        List<String> submissionIds = new ArrayList<>(submissionToTeam.keySet());
        if (submissionIds.isEmpty()) return List.of();

        Map<String, Integer> totalByTeam = new HashMap<>();
        for (Score sc : scoreRepository.findBySubmissionIdIn(submissionIds)) {
            String teamId = submissionToTeam.get(sc.getSubmissionId());
            if (teamId != null && sc.getScoreValue() != null) {
                totalByTeam.merge(teamId, sc.getScoreValue(), Integer::sum);
            }
        }
        if (totalByTeam.isEmpty()) return List.of();

        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(totalByTeam.entrySet());
        ranked.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        Map<String, String> teamNames = teamNames(totalByTeam.keySet());
        List<LeaderboardResponse> live = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Integer> entry : ranked) {
            LeaderboardResponse dto = new LeaderboardResponse();
            dto.setRank(rank++);
            dto.setTeamOrUserName(teamNames.getOrDefault(entry.getKey(), "Unknown Team"));
            dto.setTotalPoints(entry.getValue());
            live.add(dto);
        }
        return live;
    }

    // 🔥 CACHED: sum of all quiz scores per user, grouped in Java
    @Override
    @Cacheable("global-leaderboard")
    public List<LeaderboardResponse> getGlobalLeaderboard() {
        log.info("Fetching Global Leaderboard from Database (Cache Miss)");

        Map<String, Double> totals = new LinkedHashMap<>();
        for (Leaderboard b : leaderboardRepository.findByUserIdNotNull()) {
            double s = b.getScore() != null ? b.getScore() : 0;
            totals.merge(b.getUserId(), s, Double::sum);
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort(Comparator.comparingDouble((Map.Entry<String, Double> e) -> e.getValue()).reversed());

        Map<String, String> userNames = userNames(totals.keySet());
        List<LeaderboardResponse> response = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Double> e : sorted) {
            LeaderboardResponse dto = new LeaderboardResponse();
            dto.setRank(rank++);
            dto.setTeamOrUserName(userNames.getOrDefault(e.getKey(), "Unknown User"));
            dto.setTotalPoints(e.getValue().intValue());
            response.add(dto);
        }
        return response;
    }

    /** PERF helpers: resolve many display names with ONE query instead of one per row. */
    private Map<String, String> userNames(Collection<String> ids) {
        Map<String, String> map = new HashMap<>();
        List<String> clean = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!clean.isEmpty()) userRepository.findAllById(clean).forEach(u -> map.put(u.getId(), u.getName()));
        return map;
    }

    private Map<String, String> teamNames(Collection<String> ids) {
        Map<String, String> map = new HashMap<>();
        List<String> clean = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!clean.isEmpty()) teamRepository.findAllById(clean).forEach(t -> map.put(t.getId(), t.getName()));
        return map;
    }

    private List<LeaderboardResponse> mapToResponse(List<Leaderboard> boards, boolean isUser) {
        Map<String, String> userNames = isUser
                ? userNames(boards.stream().map(Leaderboard::getUserId).collect(Collectors.toList()))
                : new HashMap<>();
        Map<String, String> teamNames = !isUser
                ? teamNames(boards.stream().map(Leaderboard::getTeamId).collect(Collectors.toList()))
                : new HashMap<>();
        List<LeaderboardResponse> response = new ArrayList<>();
        int rank = 1;
        for (Leaderboard b : boards) {
            LeaderboardResponse dto = new LeaderboardResponse();
            dto.setRank(rank++);

            if (isUser) {
                String name = b.getUserId() != null
                        ? userNames.getOrDefault(b.getUserId(), "Unknown User")
                        : "Unknown User";
                dto.setTeamOrUserName(name);
                dto.setTotalPoints(b.getScore() != null ? b.getScore().intValue() : 0);
            } else {
                String name = b.getTeamId() != null
                        ? teamNames.getOrDefault(b.getTeamId(), "Unknown Team")
                        : "Unknown Team";
                dto.setTeamOrUserName(name);
                dto.setTotalPoints(b.getTotalScore() != null ? b.getTotalScore().intValue() : 0);
            }

            response.add(dto);
        }
        return response;
    }
}
