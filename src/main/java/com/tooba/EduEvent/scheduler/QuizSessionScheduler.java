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
    private final EmailService emailService; // Bug 2 fix: inject interface, not concrete class

    // Bug 2 fix: use findExpiredSessions(cutoff) instead of findByStatus("ONGOING") +
    // in-memory filtering. This lets the DB do the work and avoids saveAll() re-saving
    // every ongoing session regardless of whether it was modified.
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoSubmitExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now();
        List<QuizSession> expiredSessions = quizSessionRepository.findExpiredSessions(cutoff);

        for (QuizSession session : expiredSessions) {
            session.setScore(0.0);
            session.setEndTime(LocalDateTime.now());
            session.setStatus("COMPLETED");
            log.info("Proctor Watchdog: Auto-submitted expired session ID {} for user {}",
                    session.getId(), session.getUser().getEmail());
        }

        quizSessionRepository.saveAll(expiredSessions);
    }

    // 🕒 Task 2: Hourly notifications daemon for upcoming active benchmarks
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void sendHourlyQuizReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdLimit = now.plusHours(1);

        List<Event> imminentEvents = eventRepository.findByStartTimeBetweenAndStatus(now, thresholdLimit, "UPCOMING");

        for (Event event : imminentEvents) {
            List<Registration> activeList = registrationRepository.findByEventIdAndStatus(event.getId(), RegistrationStatus.REGISTERED);

            for (Registration enrollment : activeList) {
                try {
                    String mailBody = String.format(
                            "Greetings %s,\n\nThis is an automated notification that the quiz window for '%s' opens in less than an hour (Start Time: %s).\n\nPlease ensure your webcam testing configurations are stable before logging in.",
                            enrollment.getUser().getName(),
                            event.getTitle(),
                            event.getStartTime().toString()
                    );

                    emailService.sendEmail(enrollment.getUser().getEmail(), "Notification: Upcoming Quiz Verification Window", mailBody);
                    log.info("Notification Engine: Dispatched reminder to candidate user {}", enrollment.getUser().getEmail());
                } catch (Exception e) {
                    log.error("Notification Engine Fault: Could not dispatch alerts to user token id {}", enrollment.getUser().getId(), e);
                }
            }
        }
    }
}