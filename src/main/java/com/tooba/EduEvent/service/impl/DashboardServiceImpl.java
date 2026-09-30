package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.*;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.DashboardService;
import com.tooba.EduEvent.service.LeaderboardService;
import com.tooba.EduEvent.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final RegistrationRepository registrationRepository;
    private final QuizRepository quizRepository;
    private final QuizService quizService;
    private final CertificateRepository certificateRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationRepository notificationRepository;
    private final LeaderboardService leaderboardService;

    @Override
    public DashboardResponse getDashboard(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        String userId = user.getId();

        // 1. Registered Events
        List<Registration> registrations = registrationRepository.findByUserId(userId);
        Map<String, Event> regEvents = loadEvents(
                registrations.stream().map(Registration::getEventId).collect(Collectors.toList()));
        List<DashboardResponse.EventSummary> events = registrations.stream()
                .map(r -> {
                    Event ev = regEvents.get(r.getEventId());
                    return DashboardResponse.EventSummary.builder()
                            .eventId(r.getEventId())
                            .title(ev != null ? ev.getTitle() : "Unknown")
                            .type(ev != null ? ev.getType() : null)
                            .status(r.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());

        // 2. Upcoming Quizzes
        List<String> registeredEventIds = registrations.stream().map(Registration::getEventId).toList();
        List<DashboardResponse.QuizSummary> upcomingQuizzes = new ArrayList<>();
        if (!registeredEventIds.isEmpty()) {
            List<Quiz> quizzes = quizRepository.findByEventIdIn(registeredEventIds);
            Map<String, Event> quizEvents = loadEvents(
                    quizzes.stream().map(Quiz::getEventId).collect(Collectors.toList()));
            upcomingQuizzes = quizzes.stream()
                    .map(q -> DashboardResponse.QuizSummary.builder()
                            .quizId(q.getId())
                            .eventTitle(quizEvents.get(q.getEventId()) != null && quizEvents.get(q.getEventId()).getTitle() != null
                                    ? quizEvents.get(q.getEventId()).getTitle() : "Quiz")
                            .durationMinutes(q.getDurationMinutes())
                            .passScore(q.getPassScore().doubleValue())
                            .build())
                    .collect(Collectors.toList());
        }

        // 3. Quiz Scores
        List<QuizResultResponse> quizScores = quizService.getMyQuizHistory(email);

        // 4. Certificates
        List<Certificate> certEntities = certificateRepository.findAllByUserId(userId);
        Map<String, Event> certEvents = loadEvents(
                certEntities.stream().map(Certificate::getEventId).collect(Collectors.toList()));
        List<CertificateResponse> certificates = certEntities.stream()
                .map(cert -> CertificateResponse.builder()
                        .id(cert.getId())
                        .participantName(user.getName())
                        .eventTitle(certEvents.get(cert.getEventId()) != null && certEvents.get(cert.getEventId()).getTitle() != null
                                ? certEvents.get(cert.getEventId()).getTitle() : "Unknown Event")
                        .certUuid(cert.getCertUuid())
                        .verifyUrl(cert.getVerifyUrl())
                        .issuedAt(cert.getIssuedAt())
                        .build())
                .collect(Collectors.toList());

        // 5. My Team
        DashboardResponse.TeamSummary myTeam = null;
        Optional<TeamMember> teamMemberOpt = teamMemberRepository.findByUserId(userId).stream().findFirst();
        if (teamMemberOpt.isPresent()) {
            Team team = teamRepository.findById(teamMemberOpt.get().getTeamId()).orElse(null);
            if (team != null) {
                myTeam = DashboardResponse.TeamSummary.builder()
                        .teamId(team.getId())
                        .teamName(team.getName())
                        .hackathonTitle(eventRepository.findById(team.getHackathonId()).map(Event::getTitle).orElse("Hackathon"))
                        .build();
            }
        }

        // 6. Notifications
        List<DashboardResponse.NotificationSummary> notifications = notificationRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> DashboardResponse.NotificationSummary.builder()
                        .id(n.getId())
                        .message(n.getMessage())
                        .timeAgo(n.getCreatedAt() != null ? n.getCreatedAt().toLocalDate().toString() : "")
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

        return buildDashboard(events, upcomingQuizzes, quizScores, certificates, myTeam, notifications, myRank, totalPoints);
    }

    /** One query for many events instead of one findById per row. */
    private Map<String, Event> loadEvents(Collection<String> ids) {
        Map<String, Event> map = new HashMap<>();
        List<String> clean = ids.stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (clean.isEmpty()) return map;
        for (Event e : eventRepository.findAllById(clean)) map.put(e.getId(), e);
        return map;
    }

    private DashboardResponse buildDashboard(List<DashboardResponse.EventSummary> events,
                                             List<DashboardResponse.QuizSummary> upcomingQuizzes,
                                             List<QuizResultResponse> quizScores,
                                             List<CertificateResponse> certificates,
                                             DashboardResponse.TeamSummary myTeam,
                                             List<DashboardResponse.NotificationSummary> notifications,
                                             Integer myRank, Integer totalPoints) {
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
