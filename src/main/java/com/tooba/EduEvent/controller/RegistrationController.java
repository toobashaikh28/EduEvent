package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.RegistrationService;
import com.tooba.EduEvent.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final WaitlistService waitlistService;
    private final UserRepository userRepository;

    // POST /api/events/{id}/register — Register the logged-in user for an event
    @PostMapping("/events/{id}/register")
    public ResponseEntity<RegistrationResponse> registerToEvent(
            @PathVariable("id") Long eventId,
            Authentication authentication) {

        Long userId = resolveUserId(authentication);
        RegistrationResponse response = registrationService.registerUserToEvent(userId, eventId);

        // 201 CREATED for confirmed seat, 202 ACCEPTED for waitlist
        HttpStatus httpStatus = "WAITLISTED".equals(response.getStatus())
                ? HttpStatus.ACCEPTED
                : HttpStatus.CREATED;

        return ResponseEntity.status(httpStatus).body(response);
    }

    // DELETE /api/events/{id}/register — Cancel the logged-in user's registration
    @DeleteMapping("/events/{id}/register")
    public ResponseEntity<String> cancelRegistration(
            @PathVariable("id") Long eventId,
            Authentication authentication) {

        Long userId = resolveUserId(authentication);
        registrationService.cancelRegistration(userId, eventId);
        return ResponseEntity.ok("Registration cancelled successfully.");
    }

    // GET /api/events/{id}/registrations — Admin only: see all registrations for an event
    @GetMapping("/events/{id}/registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegistrationResponse>> getEventRegistrations(
            @PathVariable("id") Long eventId) {

        return ResponseEntity.ok(registrationService.getRegistrationsByEvent(eventId));
    }

    // GET /api/events/{id}/waitlist — Admin only: view current waitlist with positions
    @GetMapping("/events/{id}/waitlist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegistrationResponse>> getEventWaitlist(
            @PathVariable("id") Long eventId) {

        return ResponseEntity.ok(waitlistService.getWaitlistByEvent(eventId));
    }

    // GET /api/users/me/registrations — Logged-in user sees their own registrations
    @GetMapping("/users/me/registrations")
    public ResponseEntity<List<RegistrationResponse>> getMyRegistrations(
            Authentication authentication) {

        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(registrationService.getRegistrationsByUser(userId));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
        return user.getId();
    }
}