package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.entity.SentNotification;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.SentNotificationRepository;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.EmailService;
import com.tooba.EduEvent.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Admin-only messaging: broadcast in-app notifications (optionally as email)
 * to a target audience, or message a specific person by email.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminMessageController {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SentNotificationRepository sentNotificationRepository;

    // POST /api/admin/broadcast  { title, message, target: all|participants|judges|admins, email: true|false }
    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> broadcast(@RequestBody Map<String, Object> body,
                                                          Authentication authentication) {
        String title   = str(body.get("title"));
        String message = str(body.get("message"));
        String target  = str(body.get("target")).toLowerCase();
        boolean email  = Boolean.parseBoolean(String.valueOf(body.get("email")));

        if (title.isBlank() || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title and message are required");
        }

        User sender = currentUser(authentication);

        String roleFilter = switch (target) {
            case "participants" -> "USER";
            case "judges"       -> "JUDGE";
            case "admins"       -> "ADMIN";
            default              -> null; // all
        };

        List<User> recipients = userRepository.findAll().stream()
                .filter(u -> roleFilter == null || roleFilter.equalsIgnoreCase(u.getRole()))
                .filter(u -> !u.getId().equals(sender.getId())) // never send the broadcast back to the sender
                .toList();

        int notified = 0, emailed = 0;
        for (User u : recipients) {
            notificationService.send(u.getId(), title, message);
            notified++;
            if (email && u.getEmail() != null && !u.getEmail().isBlank()) {
                try { emailService.sendEmail(u.getEmail(), "EduEvent — " + title, message); emailed++; }
                catch (Exception e) { log.warn("Broadcast email to {} failed: {}", u.getEmail(), e.getMessage()); }
            }
        }

        logSent(sender.getId(), title, message, target.isBlank() ? "all" : target, notified, emailed);
        log.info("Broadcast '{}' to {} users (target={}, emailed={})", title, notified, target, emailed);
        return ResponseEntity.ok(Map.of("notified", notified, "emailed", emailed));
    }

    // GET /api/admin/sent — the broadcasts this admin has sent, newest first
    @GetMapping("/sent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SentNotification>> sentHistory(Authentication authentication) {
        User sender = currentUser(authentication);
        return ResponseEntity.ok(
                sentNotificationRepository.findTop50BySenderIdOrderByCreatedAtDesc(sender.getId()));
    }

    // POST /api/admin/message-by-email  { email, title, message, sendEmail: true|false }
    @PostMapping("/message-by-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> messageByEmail(@RequestBody Map<String, Object> body,
                                                              Authentication authentication) {
        String email   = str(body.get("email"));
        String title   = str(body.get("title"));
        String message = str(body.get("message"));
        boolean sendEmail = Boolean.parseBoolean(String.valueOf(body.get("sendEmail")));

        if (email.isBlank() || title.isBlank() || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email, title and message are required");
        }

        User sender = currentUser(authentication);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No user found with email: " + email));

        notificationService.send(user.getId(), title, message);
        boolean emailed = false;
        if (sendEmail) {
            try { emailService.sendEmail(user.getEmail(), "EduEvent — " + title, message); emailed = true; }
            catch (Exception e) { log.warn("Email to {} failed: {}", email, e.getMessage()); }
        }

        logSent(sender.getId(), title, message, email, 1, emailed ? 1 : 0);
        return ResponseEntity.ok(Map.of("notified", 1, "emailed", emailed));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
    }

    private void logSent(String senderId, String title, String message,
                         String target, int recipientCount, int emailedCount) {
        sentNotificationRepository.save(SentNotification.builder()
                .senderId(senderId)
                .title(title)
                .message(message)
                .target(target)
                .recipientCount(recipientCount)
                .emailedCount(emailedCount)
                .build());
    }

    private String str(Object o) { return o == null ? "" : o.toString().trim(); }
}
