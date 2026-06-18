package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.AnalyticsOverviewResponse;
import com.tooba.EduEvent.dto.response.AnalyticsResponse;
import com.tooba.EduEvent.dto.response.CityBreakdownResponse;
import com.tooba.EduEvent.dto.response.QuizStatsResponse;
import java.util.List;

public interface AnalyticsService {
    List<AnalyticsResponse> getRegistrationCounts();
    List<QuizStatsResponse> getQuizStats();
    List<AnalyticsResponse> getCertificateCounts();
    List<AnalyticsResponse> getDropoutCounts();
    List<AnalyticsResponse> getSubmissionCounts();

    /** Everything the admin Analytics page needs, computed from real data. */
    AnalyticsOverviewResponse getOverview(int days);

    /** Registered users grouped by city (desc) — powers the city map. */
    List<CityBreakdownResponse> getCitiesBreakdown();
}