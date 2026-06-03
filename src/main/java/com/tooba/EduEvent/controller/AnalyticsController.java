package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.response.AnalyticsResponse;
import com.tooba.EduEvent.dto.response.QuizStatsResponse;
import com.tooba.EduEvent.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AnalyticsResponse>> getRegistrations() {
        return ResponseEntity.ok(analyticsService.getRegistrationCounts());
    }

    @GetMapping("/quiz-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<QuizStatsResponse>> getQuizStats() {
        return ResponseEntity.ok(analyticsService.getQuizStats());
    }

    @GetMapping("/certificates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AnalyticsResponse>> getCertificates() {
        return ResponseEntity.ok(analyticsService.getCertificateCounts());
    }

    @GetMapping("/dropouts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AnalyticsResponse>> getDropouts() {
        return ResponseEntity.ok(analyticsService.getDropoutCounts());
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AnalyticsResponse>> getSubmissions() {
        return ResponseEntity.ok(analyticsService.getSubmissionCounts());
    }
}