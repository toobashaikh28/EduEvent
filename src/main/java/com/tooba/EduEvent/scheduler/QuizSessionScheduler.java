package com.tooba.EduEvent.scheduler;

import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.QuizSession;
import com.tooba.EduEvent.entity.Registration;
import com.tooba.EduEvent.entity.RegistrationStatus;
import com.tooba.EduEvent.repository.EventRepository;
import com.tooba.EduEvent.repository.QuizSessionRepository;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.service.impl.EmailServiceImpl; // Adjust class name to your real EmailService interface/impl
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
    private final EmailServiceImpl emailService; 

    // 🕒 Task 1: Auto-submit expired ONGOING sessions every 5 minutes
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoSubmitExpiredSessions() {
        List<QuizSession> ongoing = quizSessionRepository.findByStatus("ONGOING");

        for (QuizSession session : ongoing) {
            long minutesElapsed = java.time.Duration.between(
                    session.getStartTime(), LocalDateTime.now()).toMinutes();
            
            if (minutesElapsed > session.getQuiz().getDurationMinutes()) {
                session.setScore(0.0);
                session.setEndTime(LocalDateTime.now());
                session.setStatus("COMPLETED");
                log.info("Proctor Watchdog: Auto-submitted expired session ID {} for user {}",
                        session.getId(), session.getUser().getEmail());
            }
        }
        quizSessionRepository.saveAll(ongoing);
    }

    // 🕒 Task 2: Hourly notifications daemon for upcoming active benchmarks
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void sendHourlyQuizReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdLimit = now.plusHours(1);

        // Fetch events slated to transform status from UPCOMING in the next hour
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