package com.tooba.EduEvent.scheduler;

import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.QuizSession;
import com.tooba.EduEvent.entity.Registration;
import com.tooba.EduEvent.entity.RegistrationStatus;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.EventRepository;
import com.tooba.EduEvent.repository.QuizRepository;
import com.tooba.EduEvent.repository.QuizSessionRepository;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuizSessionScheduler {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;

    /**
     * Auto-submit ONGOING sessions whose (startTime + durationMinutes) has passed.
     */
    @Scheduled(fixedRate = 300000)
    public void autoSubmitExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        List<QuizSession> expiredSessions = quizSessionRepository
                .findByStatus("ONGOING")
                .stream()
                .filter(s -> {
                    int duration = quizRepository.findById(s.getQuizId())
                            .map(q -> q.getDurationMinutes() != null ? q.getDurationMinutes() : 30)
                            .orElse(30);
                    return s.getStartTime().plusMinutes(duration).isBefore(now);
                })
                .toList();

        for (QuizSession session : expiredSessions) {
            session.setScore(null);
            session.setEndTime(now);
            session.setStatus("TIMED_OUT");
            String email = userRepository.findById(session.getUserId()).map(User::getEmail).orElse("unknown");
            log.info("Auto-submitted timed-out session ID {} for user {}", session.getId(), email);
        }

        quizSessionRepository.saveAll(expiredSessions);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void sendHourlyQuizReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(1);

        List<Event> imminentEvents = eventRepository
                .findByStartTimeBetweenAndStatus(now, threshold, "UPCOMING");

        for (Event event : imminentEvents) {
            List<Registration> registrations = registrationRepository
                    .findByEventIdAndStatus(event.getId(), RegistrationStatus.REGISTERED);

            for (Registration enrollment : registrations) {
                User user = userRepository.findById(enrollment.getUserId()).orElse(null);
                if (user == null || user.getEmail() == null) continue;
                try {
                    String mailBody = String.format(
                            "Greetings %s,\n\nThe quiz window for '%s' opens in less than an hour (Start Time: %s).\n\nPlease ensure your browser is ready before logging in.",
                            user.getName(), event.getTitle(), event.getStartTime());
                    emailService.sendEmail(user.getEmail(), "Upcoming Quiz Reminder", mailBody);
                    log.info("Reminder sent to {}", user.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send reminder to user id {}", enrollment.getUserId(), e);
                }
            }
        }
    }
}
