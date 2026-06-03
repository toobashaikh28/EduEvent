package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.AnalyticsResponse;
import com.tooba.EduEvent.dto.response.QuizStatsResponse;
import java.util.List;

public interface AnalyticsService {
    List<AnalyticsResponse> getRegistrationCounts();
    List<QuizStatsResponse> getQuizStats();
    List<AnalyticsResponse> getCertificateCounts();
    List<AnalyticsResponse> getDropoutCounts();
    List<AnalyticsResponse> getSubmissionCounts();
}