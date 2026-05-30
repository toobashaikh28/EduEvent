package com.tooba.EduEvent.scheduler;

import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.QuizSession;
import com.tooba.EduEvent.entity.Registration;
import com.tooba.EduEvent.entity.RegistrationStatus;
import com.tooba.EduEvent.repository.EventRepository;
import com.tooba.EduEvent.repository.QuizSessionRepository;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuizSessionScheduler {

    private final QuizSessionRepository quizSessionRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;

    /**
     * FIX 2: Fetch all ONGOING sessions, then filter in Java by comparing
     * startTime + durationMinutes against now. This avoids DB-specific
     * DATEADD/TIMESTAMPADD functions that break on H2 (used in tests).
     *
     * FIX 6: Status set to TIMED_OUT, score left as null.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoSubmitExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        // Load all ONGOING sessions, filter expired ones in Java
        List<QuizSession> expiredSessions = quizSessionRepository
                .findTrulyExpiredSessions(now)
                .stream()
                .filter(s -> {
                    int duration = s.getQuiz().getDurationMinutes() != null
                            ? s.getQuiz().getDurationMinutes() : 30;
                    return s.getStartTime().plusMinutes(duration).isBefore(now);
                })
                .toList();

        for (QuizSession session : expiredSessions) {
            session.setScore(null);
            session.setEndTime(now);
            session.setStatus("TIMED_OUT");
            log.info("Auto-submitted timed-out session ID {} for user {}",
                    session.getId(), session.getUser().getEmail());
        }

        quizSessionRepository.saveAll(expiredSessions);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void sendHourlyQuizReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(1);

        List<Event> imminentEvents = eventRepository
                .findByStartTimeBetweenAndStatus(now, threshold, "UPCOMING");

        for (Event event : imminentEvents) {
            List<Registration> registrations = registrationRepository
                    .findByEventIdAndStatus(event.getId(), RegistrationStatus.REGISTERED);

            for (Registration enrollment : registrations) {
                try {
                    String mailBody = String.format(
                            "Greetings %s,\n\nThe quiz window for '%s' opens in less than an hour (Start Time: %s).\n\nPlease ensure your browser is ready before logging in.",
                            enrollment.getUser().getName(),
                            event.getTitle(),
                            event.getStartTime()
                    );
                    emailService.sendEmail(
                            enrollment.getUser().getEmail(),
                            "Upcoming Quiz Reminder",
                            mailBody);
                    log.info("Reminder sent to {}", enrollment.getUser().getEmail());
                } catch (Exception e) {
                    log.error("Failed to send reminder to user id {}", enrollment.getUser().getId(), e);
                }
            }
        }
    }
}