package com.tooba.EduEvent.dto.response;

import lombok.*;

import java.util.List;

/**
 * One-shot payload for the admin Analytics page — every card, chart and table
 * computed from real data so the frontend needs a single request.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalyticsOverviewResponse {

    // Summary cards
    private long totalRegistrations;
    private double passRate;        // % (0-100)
    private double avgQuizScore;    // 0-100
    private long certificatesIssued;
    private long dropouts;

    // Registrations over time (line)
    private List<String> regTimeLabels;
    private List<Long> regTimeValues;

    // Event-type distribution (doughnut) — registrations grouped by event type
    private List<String> typeLabels;
    private List<Long> typeValues;

    // Quiz score distribution (bar) — five fixed bands
    private List<String> scoreBandLabels;
    private List<Long> scoreBandValues;

    // Monthly certificates issued (bar)
    private List<String> certMonthLabels;
    private List<Long> certMonthValues;

    // Top events by registration (table)
    private List<TopEvent> topEvents;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopEvent {
        private String name;
        private String type;
        private long registrations;
        private int fillRate;       // % (0-100)
    }
}
