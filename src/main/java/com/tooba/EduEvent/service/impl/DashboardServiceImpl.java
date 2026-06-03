package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.*;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.DashboardService;
import com.tooba.EduEvent.service.LeaderboardService;
import com.tooba.EduEvent.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final QuizRepository quizRepository;
    private final QuizService quizService;
    private final CertificateRepository certificateRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationRepository notificationRepository;
    private final LeaderboardService leaderboardService;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Long userId = user.getId();

        // 1. Registered Events
        List<Registration> registrations = registrationRepository.findByUserId(userId);
        List<DashboardResponse.EventSummary> events = registrations.stream()
                .map(r -> DashboardResponse.EventSummary.builder()
                        .eventId(r.getEvent().getId())
                        .title(r.getEvent().getTitle())
                        .type(r.getEvent().getType())
                        .status(r.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        // 2. Upcoming Quizzes
        List<Long> registeredEventIds = registrations.stream().map(r -> r.getEvent().getId()).toList();
        List<DashboardResponse.QuizSummary> upcomingQuizzes = new ArrayList<>();
        if (!registeredEventIds.isEmpty()) {
            upcomingQuizzes = quizRepository.findByEventIdIn(registeredEventIds).stream()
                    .map(q -> DashboardResponse.QuizSummary.builder()
                            .quizId(q.getId())
                            .eventTitle(q.getEvent().getTitle())
                            .durationMinutes(q.getDurationMinutes())
                            .passScore(q.getPassScore().doubleValue())
                            .build())
                    .collect(Collectors.toList());
        }

        // 3. Quiz Scores (Reusing QuizService)
        List<QuizResultResponse> quizScores = quizService.getMyQuizHistory(email);

        // 4. Certificates
        List<CertificateResponse> certificates = certificateRepository.findAllByUserId(userId).stream()
                .map(cert -> CertificateResponse.builder()
                        .id(cert.getId())
                        .participantName(cert.getUser().getName())
                        .eventTitle(cert.getEvent().getTitle())
                        .certUuid(cert.getCertUuid())
                        .verifyUrl(cert.getVerifyUrl())
                        .issuedAt(cert.getIssuedAt())
                        .build())
                .collect(Collectors.toList());

        // 5. My Team
        DashboardResponse.TeamSummary myTeam = null;
        Optional<TeamMember> teamMemberOpt = teamMemberRepository.findByUserId(userId).stream().findFirst();
        if (teamMemberOpt.isPresent()) {
            Team team = teamMemberOpt.get().getTeam();
            myTeam = DashboardResponse.TeamSummary.builder()
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .hackathonTitle(team.getHackathon().getTitle())
                    .build();
        }

        // 6. Notifications
        List<DashboardResponse.NotificationSummary> notifications = notificationRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> DashboardResponse.NotificationSummary.builder()
                        .id(n.getId())
                        .message(n.getMessage())
                        .timeAgo(n.getCreatedAt().toLocalDate().toString())
                        .build())
                .collect(Collectors.toList());

        // 7. Global Rank
        Integer myRank = null;
        Integer totalPoints = 0;
        for (LeaderboardResponse entry : leaderboardService.getGlobalLeaderboard()) {
            if (entry.getTeamOrUserName().equals(user.getName())) {
                myRank = entry.getRank();
                totalPoints = entry.getTotalPoints();
                break;
            }
        }

        return DashboardResponse.builder()
                .registeredEvents(events)
                .upcomingQuizzes(upcomingQuizzes)
                .quizScores(quizScores)
                .certificates(certificates)
                .myTeam(myTeam)
                .recentNotifications(notifications)
                .myRank(myRank)
                .totalPoints(totalPoints)
                .build();
    }
}