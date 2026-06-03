package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.AnalyticsResponse;
import com.tooba.EduEvent.dto.response.QuizStatsResponse;
import com.tooba.EduEvent.repository.CertificateRepository;
import com.tooba.EduEvent.repository.QuizSessionRepository;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.repository.SubmissionRepository;
import com.tooba.EduEvent.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final RegistrationRepository registrationRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final CertificateRepository certificateRepository;
    private final SubmissionRepository submissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AnalyticsResponse> getRegistrationCounts() {
        return registrationRepository.countRegistrationsPerEvent().stream()
                .map(obj -> AnalyticsResponse.builder()
                        .eventName((String) obj[0])
                        .count((Long) obj[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizStatsResponse> getQuizStats() {
        return quizSessionRepository.getQuizStats().stream()
                .map(obj -> QuizStatsResponse.builder()
                        .quizId((Long) obj[0])
                        .averageScore((Double) obj[1])
                        // Rounding the percentage to 2 decimal places
                        .passRatePercentage(obj[2] != null ? Math.round((Double) obj[2] * 100.0) / 100.0 : 0.0) 
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyticsResponse> getCertificateCounts() {
        return certificateRepository.countCertificatesPerEvent().stream()
                .map(obj -> AnalyticsResponse.builder()
                        .eventName((String) obj[0])
                        .count((Long) obj[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyticsResponse> getDropoutCounts() {
        return registrationRepository.countDropoutsPerEvent().stream()
                .map(obj -> AnalyticsResponse.builder()
                        .eventName((String) obj[0])
                        .count((Long) obj[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyticsResponse> getSubmissionCounts() {
        return submissionRepository.countSubmissionsPerHackathon().stream()
                .map(obj -> AnalyticsResponse.builder()
                        .eventName((String) obj[0])
                        .count((Long) obj[1])
                        .build())
                .collect(Collectors.toList());
    }
}