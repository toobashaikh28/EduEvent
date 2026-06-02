package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.LeaderboardResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HackathonServiceImpl implements HackathonService {

    private final LeaderboardRepository leaderboardRepository;
    private final EventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CertificateService certificateService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void announceWinners(Long hackathonId) {
        if (leaderboardRepository.existsByHackathonId(hackathonId)) {
            throw new RuntimeException("Winners already announced for this hackathon");
        }

        Event hackathon = eventRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon not found"));

        List<Object[]> scores = leaderboardRepository.aggregateScoresByTeam(hackathonId);
        int rank = 1;

        for (Object[] row : scores) {
            Long teamId = ((Number) row[0]).longValue();
            BigDecimal totalScore = new BigDecimal(row[1].toString());

            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new RuntimeException("Team not found"));

            Leaderboard leaderboard = Leaderboard.builder()
                    .hackathon(hackathon)
                    .team(team)
                    .rank(rank)
                    .totalScore(totalScore)
                    .build();
            leaderboardRepository.save(leaderboard);

            List<TeamMember> members = teamMemberRepository.findAllByTeamIdAndStatus(teamId, "ACCEPTED");

            if (rank <= 3) {
                for (TeamMember member : members) {
                    certificateService.generate(member.getUser(), hackathon, rank);
                }
            }

            for (TeamMember member : members) {
                String message = "The results are in! Your team placed #" + rank + " in " + hackathon.getTitle() + ".";
                notificationService.send(member.getUser().getId(), "Hackathon Results Announced", message);
            }

            rank++;
        }
    }

    @Override
    public List<LeaderboardResponse> getResults(Long hackathonId) {
        List<Leaderboard> leaderboards = leaderboardRepository.findAllByHackathonIdOrderByRankAsc(hackathonId);
        List<LeaderboardResponse> responses = new ArrayList<>();
        for (Leaderboard l : leaderboards) {
            LeaderboardResponse resp = new LeaderboardResponse();
            resp.setRank(l.getRank());
            resp.setTeamOrUserName(l.getTeam().getName());
            resp.setTotalPoints(l.getTotalScore().intValue());
            responses.add(resp);
        }
        return responses;
    }
}
