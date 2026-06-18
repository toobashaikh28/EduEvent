package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.response.NotificationResponse;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // GET /api/notifications — returns user's notifications, unread first
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication authentication) {
        String userId = resolveUserId(authentication);
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }

    // PUT /api/notifications/{id}/read — mark a notification as read
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String id,
            Authentication authentication) {
        String userId = resolveUserId(authentication);
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    private String resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
        return user.getId();
    }
}