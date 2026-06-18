package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.AnalyticsOverviewResponse;
import com.tooba.EduEvent.dto.response.AnalyticsResponse;
import com.tooba.EduEvent.dto.response.CityBreakdownResponse;
import com.tooba.EduEvent.dto.response.QuizStatsResponse;
import com.tooba.EduEvent.entity.Certificate;
import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.Quiz;
import com.tooba.EduEvent.entity.QuizSession;
import com.tooba.EduEvent.entity.Registration;
import com.tooba.EduEvent.entity.RegistrationStatus;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final RegistrationRepository registrationRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizRepository quizRepository;
    private final CertificateRepository certificateRepository;
    private final SubmissionRepository submissionRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private String eventTitle(String eventId) {
        return eventRepository.findById(eventId).map(Event::getTitle).orElse("Unknown");
    }

    // Group a stream of eventIds into AnalyticsResponse(title, count)
    private List<AnalyticsResponse> countByEvent(Stream<String> eventIds) {
        Map<String, Long> counts = new LinkedHashMap<>();
        eventIds.forEach(id -> counts.merge(id, 1L, Long::sum));
        return counts.entrySet().stream()
                .map(e -> AnalyticsResponse.builder()
                        .eventName(eventTitle(e.getKey()))
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsResponse> getRegistrationCounts() {
        return countByEvent(registrationRepository.findByStatus(RegistrationStatus.REGISTERED)
                .stream().map(Registration::getEventId));
    }

    @Override
    public List<QuizStatsResponse> getQuizStats() {
        Map<String, List<Double>> scoresByQuiz = new LinkedHashMap<>();
        quizSessionRepository.findByStatus("COMPLETED").forEach(s -> {
            if (s.getScore() != null) {
                scoresByQuiz.computeIfAbsent(s.getQuizId(), k -> new ArrayList<>()).add(s.getScore());
            }
        });

        List<QuizStatsResponse> out = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : scoresByQuiz.entrySet()) {
            List<Double> scores = e.getValue();
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double pass = quizRepository.findById(e.getKey())
                    .map(Quiz::getPassScore).map(java.math.BigDecimal::doubleValue).orElse(50.0);
            long passed = scores.stream().filter(sc -> sc >= pass).count();
            double passRate = scores.isEmpty() ? 0 : ((double) passed / scores.size()) * 100;
            out.add(QuizStatsResponse.builder()
                    .quizId(e.getKey())
                    .averageScore(avg)
                    .passRatePercentage(Math.round(passRate * 100.0) / 100.0)
                    .build());
        }
        return out;
    }

    @Override
    public List<AnalyticsResponse> getCertificateCounts() {
        return countByEvent(certificateRepository.findAll().stream()
                .map(com.tooba.EduEvent.entity.Certificate::getEventId));
    }

    @Override
    public List<AnalyticsResponse> getDropoutCounts() {
        return countByEvent(Stream.concat(
                registrationRepository.findByStatus(RegistrationStatus.CANCELLED).stream(),
                registrationRepository.findByStatus(RegistrationStatus.WAITLISTED).stream()
        ).map(Registration::getEventId));
    }

    @Override
    public List<AnalyticsResponse> getSubmissionCounts() {
        return countByEvent(submissionRepository.findAll().stream()
                .map(com.tooba.EduEvent.entity.Submission::getHackathonId));
    }

    @Override
    public List<CityBreakdownResponse> getCitiesBreakdown() {
        return userRepository.findAll().stream()
                .map(User::getCity)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .collect(Collectors.groupingBy(this::titleCity, Collectors.counting()))
                .entrySet().stream()
                .map(e -> CityBreakdownResponse.builder().city(e.getKey()).count(e.getValue()).build())
                .sorted(Comparator.comparingLong(CityBreakdownResponse::getCount).reversed())
                .collect(Collectors.toList());
    }

    private String titleCity(String c) {
        String t = c.trim();
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1).toLowerCase();
    }

    @Override
    public AnalyticsOverviewResponse getOverview(int days) {
        int window = (days <= 0) ? 30 : days;

        List<Event> events = eventRepository.findAll();
        Map<String, Event> eventById = events.stream()
                .collect(Collectors.toMap(Event::getId, e -> e, (a, b) -> a, LinkedHashMap::new));

        List<Registration> registered = registrationRepository.findByStatus(RegistrationStatus.REGISTERED);
        List<Certificate> certificates = certificateRepository.findAll();
        List<QuizSession> completedSessions = quizSessionRepository.findByStatus("COMPLETED");

        // ── Summary cards ──────────────────────────────────────────────
        long totalRegistrations = registered.size();
        long certificatesIssued = certificates.size();
        long dropouts = registrationRepository.findByStatus(RegistrationStatus.CANCELLED).size()
                + registrationRepository.findByStatus(RegistrationStatus.WAITLISTED).size();

        List<Double> scores = completedSessions.stream()
                .map(QuizSession::getScore).filter(java.util.Objects::nonNull).toList();
        double avgQuizScore = round1(scores.stream().mapToDouble(Double::doubleValue).average().orElse(0));

        // Overall pass rate, using each quiz's own pass score (default 50)
        Map<String, Double> passScoreByQuiz = quizRepository.findAll().stream()
                .collect(Collectors.toMap(Quiz::getId,
                        q -> q.getPassScore() != null ? q.getPassScore().doubleValue() : 50.0,
                        (a, b) -> a));
        long passed = completedSessions.stream()
                .filter(s -> s.getScore() != null)
                .filter(s -> s.getScore() >= passScoreByQuiz.getOrDefault(s.getQuizId(), 50.0))
                .count();
        double passRate = completedSessions.isEmpty() ? 0
                : round1((double) passed / completedSessions.size() * 100);

        // ── Registrations over time (last `window` days) ───────────────
        Map<LocalDate, Long> regByDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(window - 1L);
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) regByDay.put(d, 0L);
        for (Registration r : registered) {
            if (r.getRegisteredAt() == null) continue;
            LocalDate d = r.getRegisteredAt().toLocalDate();
            if (regByDay.containsKey(d)) regByDay.merge(d, 1L, Long::sum);
        }
        List<String> regTimeLabels = regByDay.keySet().stream()
                .map(d -> d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + d.getDayOfMonth())
                .collect(Collectors.toList());
        List<Long> regTimeValues = new ArrayList<>(regByDay.values());

        // ── Event-type distribution (registrations grouped by event type) ─
        Map<String, Long> byType = new LinkedHashMap<>();
        for (Registration r : registered) {
            Event ev = eventById.get(r.getEventId());
            String type = (ev != null && ev.getType() != null && !ev.getType().isBlank())
                    ? capitalize(ev.getType()) : "Other";
            byType.merge(type, 1L, Long::sum);
        }
        List<Map.Entry<String, Long>> typeSorted = byType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).toList();
        List<String> typeLabels = typeSorted.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        List<Long> typeValues = typeSorted.stream().map(Map.Entry::getValue).collect(Collectors.toList());

        // ── Quiz score distribution (five fixed bands) ─────────────────
        List<String> scoreBandLabels = List.of("0–20", "21–40", "41–60", "61–80", "81–100");
        long[] bands = new long[5];
        for (double s : scores) {
            int idx = (s <= 20) ? 0 : (s <= 40) ? 1 : (s <= 60) ? 2 : (s <= 80) ? 3 : 4;
            bands[idx]++;
        }
        List<Long> scoreBandValues = List.of(bands[0], bands[1], bands[2], bands[3], bands[4]);

        // ── Monthly certificates issued (last 7 months) ────────────────
        Map<YearMonth, Long> certByMonth = new LinkedHashMap<>();
        YearMonth thisMonth = YearMonth.now();
        for (int i = 6; i >= 0; i--) certByMonth.put(thisMonth.minusMonths(i), 0L);
        for (Certificate c : certificates) {
            if (c.getIssuedAt() == null) continue;
            YearMonth ym = YearMonth.from(c.getIssuedAt());
            if (certByMonth.containsKey(ym)) certByMonth.merge(ym, 1L, Long::sum);
        }
        List<String> certMonthLabels = certByMonth.keySet().stream()
                .map(ym -> ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .collect(Collectors.toList());
        List<Long> certMonthValues = new ArrayList<>(certByMonth.values());

        // ── Top events by registration ─────────────────────────────────
        Map<String, Long> regByEvent = registered.stream()
                .collect(Collectors.groupingBy(Registration::getEventId, Collectors.counting()));
        List<AnalyticsOverviewResponse.TopEvent> topEvents = regByEvent.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Event ev = eventById.get(e.getKey());
                    long regs = e.getValue();
                    int cap = (ev != null && ev.getCapacity() != null && ev.getCapacity() > 0) ? ev.getCapacity() : 0;
                    int fill = cap > 0 ? (int) Math.min(100, Math.round((double) regs / cap * 100)) : 0;
                    return AnalyticsOverviewResponse.TopEvent.builder()
                            .name(ev != null ? ev.getTitle() : "Unknown")
                            .type(ev != null && ev.getType() != null ? ev.getType().toLowerCase() : "event")
                            .registrations(regs)
                            .fillRate(fill)
                            .build();
                })
                .collect(Collectors.toList());

        return AnalyticsOverviewResponse.builder()
                .totalRegistrations(totalRegistrations)
                .passRate(passRate)
                .avgQuizScore(avgQuizScore)
                .certificatesIssued(certificatesIssued)
                .dropouts(dropouts)
                .regTimeLabels(regTimeLabels)
                .regTimeValues(regTimeValues)
                .typeLabels(typeLabels)
                .typeValues(typeValues)
                .scoreBandLabels(scoreBandLabels)
                .scoreBandValues(scoreBandValues)
                .certMonthLabels(certMonthLabels)
                .certMonthValues(certMonthValues)
                .topEvents(topEvents)
                .build();
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private static String capitalize(String s) {
        String t = s.trim();
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1).toLowerCase();
    }
}
