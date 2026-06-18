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
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final CertificateService certificateService;
    private final NotificationService notificationService;
    private final NotificationMediator notificationMediator;

    private String userName(String userId) {
        return userRepository.findById(userId).map(User::getName).orElse("Unknown");
    }

    @Override
    public WinnerAnnouncementResponse announceWinners(String hackathonId, String adminEmail) {

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!"ADMIN".equals(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can announce winners");
        }

        Event hackathon = eventRepository.findById(hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hackathon not found"));
        if (!"Hackathon".equalsIgnoreCase(hackathon.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not a hackathon");
        }

        if (leaderboardRepository.existsByHackathonId(hackathonId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Winners have already been announced for this hackathon");
        }

        // Aggregate scores by team (in Java, since Mongo has no JPA joins)
        List<Submission> subs = submissionRepository.findAllByHackathonId(hackathonId);
        Map<String, String> submissionToTeam = subs.stream()
                .collect(Collectors.toMap(Submission::getId, Submission::getTeamId, (a, b) -> a));
        List<String> submissionIds = new ArrayList<>(submissionToTeam.keySet());

        Map<String, Integer> totalByTeam = new HashMap<>();
        if (!submissionIds.isEmpty()) {
            for (Score sc : scoreRepository.findBySubmissionIdIn(submissionIds)) {
                String teamId = submissionToTeam.get(sc.getSubmissionId());
                if (teamId != null && sc.getScoreValue() != null) {
                    totalByTeam.merge(teamId, sc.getScoreValue(), Integer::sum);
                }
            }
        }
        if (totalByTeam.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No scores found for this hackathon. Ensure judges have submitted scores first.");
        }

        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(totalByTeam.entrySet());
        ranked.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<WinnerAnnouncementResponse.RankedTeam> rankings = new ArrayList<>();
        List<Leaderboard> leaderboardEntries = new ArrayList<>();

        for (int i = 0; i < ranked.size(); i++) {
            String teamId = ranked.get(i).getKey();
            BigDecimal totalScore = BigDecimal.valueOf(ranked.get(i).getValue());
            int rank = i + 1;

            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Team data inconsistency detected"));

            Leaderboard entry = Leaderboard.builder()
                    .hackathonId(hackathon.getId())
                    .teamId(team.getId())
                    .rank(rank)
                    .totalScore(totalScore)
                    .build();
            leaderboardEntries.add(entry);

            List<TeamMember> members = teamMemberRepository.findAllByTeamIdAndStatus(teamId, "ACCEPTED");

            boolean certsIssued = false;
            if (rank <= 3) {
                for (TeamMember member : members) {
                    User memberUser = userRepository.findById(member.getUserId()).orElse(null);
                    if (memberUser != null) {
                        certificateService.generate(memberUser, hackathon);
                    }
                    notificationMediator.notify(
                        this,
                        "WINNER",
                        member.getUserId(),
                        "Congratulations! Your team '" + team.getName() + "' placed #" + rank
                            + " in " + hackathon.getTitle() + "! Your certificate is ready."
                    );
                }
                certsIssued = true;
                log.info("Certificates issued for rank {} team: {}", rank, team.getName());
            }

            List<String> memberNames = members.stream()
                    .map(m -> userName(m.getUserId()))
                    .collect(Collectors.toList());

            rankings.add(WinnerAnnouncementResponse.RankedTeam.builder()
                    .rank(rank)
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .leaderName(userName(team.getLeaderId()))
                    .memberNames(memberNames)
                    .totalScore(totalScore)
                    .certificatesIssued(certsIssued)
                    .build());
        }

        leaderboardRepository.saveAll(leaderboardEntries);
        log.info("Leaderboard saved: {} teams ranked for hackathon {}", leaderboardEntries.size(), hackathonId);

        notifyAllParticipants(hackathon, rankings);

        return WinnerAnnouncementResponse.builder()
                .hackathonId(hackathonId)
                .hackathonTitle(hackathon.getTitle())
                .totalTeamsRanked(rankings.size())
                .rankings(rankings)
                .build();
    }

    private void notifyAllParticipants(Event hackathon, List<WinnerAnnouncementResponse.RankedTeam> rankings) {
        Set<String> notifiedUserIds = new HashSet<>();

        for (WinnerAnnouncementResponse.RankedTeam rankedTeam : rankings) {
            List<TeamMember> members = teamMemberRepository.findAllByTeamIdAndStatus(rankedTeam.getTeamId(), "ACCEPTED");
            String placementMsg = buildPlacementMessage(hackathon.getTitle(), rankedTeam);
            for (TeamMember member : members) {
                notificationService.send(
                        member.getUserId(),
                        "🏆 " + hackathon.getTitle() + " — Results Announced!",
                        placementMsg);
                notifiedUserIds.add(member.getUserId());
            }
        }

        registrationRepository
                .findByEventIdAndStatus(hackathon.getId(), RegistrationStatus.REGISTERED)
                .forEach(reg -> {
                    if (!notifiedUserIds.contains(reg.getUserId())) {
                        notificationService.send(
                                reg.getUserId(),
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
    public List<WinnerAnnouncementResponse.RankedTeam> getResults(String hackathonId) {
        eventRepository.findById(hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hackathon not found"));

        List<Leaderboard> entries = leaderboardRepository.findAllByHackathonIdOrderByRankAsc(hackathonId);

        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Results not yet announced for this hackathon");
        }

        return entries.stream().map(entry -> {
            Team team = teamRepository.findById(entry.getTeamId()).orElse(null);
            List<String> memberNames = teamMemberRepository
                    .findAllByTeamIdAndStatus(entry.getTeamId(), "ACCEPTED")
                    .stream()
                    .map(m -> userName(m.getUserId()))
                    .collect(Collectors.toList());

            return WinnerAnnouncementResponse.RankedTeam.builder()
                    .rank(entry.getRank())
                    .teamId(entry.getTeamId())
                    .teamName(team != null ? team.getName() : "Unknown Team")
                    .leaderName(team != null ? userName(team.getLeaderId()) : "Unknown")
                    .memberNames(memberNames)
                    .totalScore(entry.getTotalScore())
                    .certificatesIssued(entry.getRank() <= 3)
                    .build();
        }).collect(Collectors.toList());
    }
}
