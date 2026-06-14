package com.tooba.EduEvent.mediator;

import com.tooba.EduEvent.entity.Notification;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.NotificationRepository;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationMediatorImpl implements NotificationMediator {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final Set<String> EMAIL_TYPES =
        Set.of("QUIZ_PASSED", "WINNER", "CERTIFICATE_READY");

    @Override
    public void notify(Object sender, String eventType, Long targetUserId, String message) {
        System.out.println("[MEDIATOR] Received event: " + eventType
            + " from " + sender.getClass().getSimpleName());

        User user = userRepository.findById(targetUserId).orElse(null);
        if (user == null) return;

        // Always save to DB
        Notification notification = Notification.builder()
            .user(user)
            .title(resolveTitleFor(eventType))
            .message(message)
            .isRead(false)
            .build();
        notificationRepository.save(notification);

        System.out.println("[MEDIATOR] Saved to DB for user: " + user.getEmail());

        // Send email only for important events
        if (EMAIL_TYPES.contains(eventType) && user.getEmail() != null) {
            try {
                emailService.sendEmail(
                    user.getEmail(),
                    "EduEvent — " + resolveTitleFor(eventType),
                    message
                );
                System.out.println("[MEDIATOR] Email sent to: " + user.getEmail());
            } catch (Exception e) {
                System.err.println("[MEDIATOR] Email failed: " + e.getMessage());
            }
        }
    }

    private String resolveTitleFor(String eventType) {
        return switch (eventType) {
            case "QUIZ_PASSED"       -> "Quiz Passed! 🎉";
            case "QUIZ_FAILED"       -> "Quiz Result";
            case "WINNER"            -> "You Won! 🏆";
            case "CERTIFICATE_READY" -> "Certificate Ready";
            case "REGISTRATION"      -> "Registration Confirmed";
            case "WAITLISTED"        -> "Added to Waitlist";
            case "TEAM_JOINED"       -> "Team Update";
            case "EVENT_UPDATED"     -> "Event Updated";
            case "EVENT_DELETED"     -> "Event Cancelled";
            default                  -> "EduEvent Notification";
        };
    }
}
