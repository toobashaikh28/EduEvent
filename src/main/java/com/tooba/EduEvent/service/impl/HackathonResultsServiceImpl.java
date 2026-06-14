package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.WinnerAnnouncementResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.CertificateService;
import com.tooba.EduEvent.service.HackathonResultsService;
import com.tooba.EduEvent.mediator.NotificationMediator;
import com.tooba.EduEvent.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HackathonResultsServiceImpl implements HackathonResultsService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final RegistrationRepository registrationRepository;
    private final CertificateService certificateService;
    private final NotificationService notificationService;
    private final NotificationMediator notificationMediator;

    @Override
    @Transactional
    public WinnerAnnouncementResponse announceWinners(Long hackathonId, String adminEmail) {

        // 1. Validate admin
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!"ADMIN".equals(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can announce winners");
        }

        // 2. Validate hackathon
        Event hackathon = eventRepository.findById(hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hackathon not found"));
        if (!"Hackathon".equals(hackathon.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not a hackathon");
        }

        // 3. Idempotency — prevent double-announcement
        if (leaderboardRepository.existsByHackathonId(hackathonId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Winners have already been announced for this hackathon");
        }

        // 4. Aggregate: SELECT team_id, SUM(score) FROM scores GROUP BY team_id ORDER BY total DESC
        List<Object[]> aggregated = leaderboardRepository.aggregateScoresByTeam(hackathonId);
        if (aggregated.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No scores found for this hackathon. Ensure judges have submitted scores first.");
        }

        // 5. Build ranked leaderboard entries and collect ranked team data for response
        List<WinnerAnnouncementResponse.RankedTeam> rankings = new ArrayList<>();
        List<Leaderboard> leaderboardEntries = new ArrayList<>();

        for (int i = 0; i < aggregated.size(); i++) {
            Object[] row = aggregated.get(i);
            Long teamId = ((Number) row[0]).longValue();
            BigDecimal totalScore = new BigDecimal(row[1].toString());
            int rank = i + 1;

            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Team data inconsistency detected"));

            // 6. Save to leaderboard table via Leaderboard.builder()
            Leaderboard entry = Leaderboard.builder()
                    .hackathon(hackathon)
                    .team(team)
                    .rank(rank)
                    .totalScore(totalScore)
                    .build();
            leaderboardEntries.add(entry);

            // 7. Get accepted members of this team
            List<TeamMember> members = teamMemberRepository
                    .findAllByTeamIdAndStatus(teamId, "ACCEPTED");

            // 8. Issue certificates for top 3 winning teams (rank 1, 2, 3)
            boolean certsIssued = false;
            if (rank <= 3) {
                for (TeamMember member : members) {
                    certificateService.generate(member.getUser(), hackathon);
                    // ── MEDIATOR: notify each winner ─────────────────────────
                    notificationMediator.notify(
                        this,
                        "WINNER",
                        member.getUser().getId(),
                        "Congratulations! Your team '" + team.getName() + "' placed #" + rank
                            + " in " + hackathon.getTitle() + "! Your certificate is ready."
                    );
                    // ─────────────────────────────────────────────────────────
                }
                certsIssued = true;
                log.info("Certificates issued for rank {} team: {}", rank, team.getName());
            }

            List<String> memberNames = members.stream()
                    .map(m -> m.getUser().getName())
                    .collect(Collectors.toList());

            rankings.add(WinnerAnnouncementResponse.RankedTeam.builder()
                    .rank(rank)
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .leaderName(team.getLeader().getName())
                    .memberNames(memberNames)
                    .totalScore(totalScore)
                    .certificatesIssued(certsIssued)
                    .build());
        }

        leaderboardRepository.saveAll(leaderboardEntries);
        log.info("Leaderboard saved: {} teams ranked for hackathon {}", leaderboardEntries.size(), hackathonId);

        // 9. Notify ALL hackathon registrants with their team's placement
        notifyAllParticipants(hackathon, rankings);

        return WinnerAnnouncementResponse.builder()
                .hackathonId(hackathonId)
                .hackathonTitle(hackathon.getTitle())
                .totalTeamsRanked(rankings.size())
                .rankings(rankings)
                .build();
    }

    private void notifyAllParticipants(Event hackathon, List<WinnerAnnouncementResponse.RankedTeam> rankings) {
        // Build a quick lookup: teamId -> rank
        Map<Long, Integer> teamRankMap = rankings.stream()
                .collect(Collectors.toMap(
                        WinnerAnnouncementResponse.RankedTeam::getTeamId,
                        WinnerAnnouncementResponse.RankedTeam::getRank));

        // Notify every accepted team member
        for (WinnerAnnouncementResponse.RankedTeam rankedTeam : rankings) {
            List<TeamMember> members = teamMemberRepository
                    .findAllByTeamIdAndStatus(rankedTeam.getTeamId(), "ACCEPTED");

            String placementMsg = buildPlacementMessage(hackathon.getTitle(), rankedTeam);

            for (TeamMember member : members) {
                notificationService.send(
                        member.getUser().getId(),
                        "🏆 " + hackathon.getTitle() + " — Results Announced!",
                        placementMsg);
            }
        }

        // Also notify registered participants who weren't in a scored team
        registrationRepository
                .findByEventIdAndStatus(hackathon.getId(), RegistrationStatus.REGISTERED)
                .forEach(reg -> {
                    boolean alreadyNotified = rankings.stream()
                            .flatMap(r -> teamMemberRepository
                                    .findAllByTeamIdAndStatus(r.getTeamId(), "ACCEPTED").stream())
                            .anyMatch(tm -> tm.getUser().getId().equals(reg.getUser().getId()));

                    if (!alreadyNotified) {
                        notificationService.send(
                                reg.getUser().getId(),
                                hackathon.getTitle() + " — Results Announced",
                                "The results for " + hackathon.getTitle() + " have been announced. Check the leaderboard at /api/hackathon/" + hackathon.getId() + "/results");
                    }
                });
    }

    private String buildPlacementMessage(String hackathonTitle, WinnerAnnouncementResponse.RankedTeam team) {
        String suffix = switch (team.getRank()) {
            case 1 -> "🥇 1st Place — Congratulations! You won!";
            case 2 -> "🥈 2nd Place — Amazing work!";
            case 3 -> "🥉 3rd Place — Great job!";
            default -> "Rank #" + team.getRank();
        };
        return String.format(
                "Results are in for %s! Your team '%s' finished in %s. " +
                (team.getRank() <= 3 ? "Your certificate has been issued — check your profile." : "Thank you for participating!"),
                hackathonTitle, team.getTeamName(), suffix);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WinnerAnnouncementResponse.RankedTeam> getResults(Long hackathonId) {
        eventRepository.findById(hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hackathon not found"));

        List<Leaderboard> entries = leaderboardRepository.findAllByHackathonIdOrderByRankAsc(hackathonId);

        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Results not yet announced for this hackathon");
        }

        return entries.stream().map(entry -> {
            List<String> memberNames = teamMemberRepository
                    .findAllByTeamIdAndStatus(entry.getTeam().getId(), "ACCEPTED")
                    .stream()
                    .map(m -> m.getUser().getName())
                    .collect(Collectors.toList());

            return WinnerAnnouncementResponse.RankedTeam.builder()
                    .rank(entry.getRank())
                    .teamId(entry.getTeam().getId())
                    .teamName(entry.getTeam().getName())
                    .leaderName(entry.getTeam().getLeader().getName())
                    .memberNames(memberNames)
                    .totalScore(entry.getTotalScore())
                    .certificatesIssued(entry.getRank() <= 3)
                    .build();
        }).collect(Collectors.toList());
    }
}
