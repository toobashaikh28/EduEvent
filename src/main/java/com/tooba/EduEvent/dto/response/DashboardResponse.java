package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private List<EventSummary> registeredEvents;
    private List<QuizSummary> upcomingQuizzes;
    private List<QuizResultResponse> quizScores;     // Reusing your existing DTO
    private List<CertificateResponse> certificates;  // Reusing your existing DTO
    private TeamSummary myTeam;
    private List<NotificationSummary> recentNotifications;
    private Integer myRank;
    private Integer totalPoints;

    // --- NESTED SUMMARY CLASSES ---
    @Data @Builder public static class EventSummary {
        private String eventId;
        private String title;
        private String type;
        private String status;
    }

    @Data @Builder public static class QuizSummary {
        private String quizId;
        private String eventTitle;
        private Integer durationMinutes;
        private Double passScore;
    }

    @Data @Builder public static class TeamSummary {
        private String teamId;
        private String teamName;
        private String hackathonTitle;
    }

    @Data @Builder public static class NotificationSummary {
        private String id;
        private String message;
        private String timeAgo;
    }
}