package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.request.EventRequest;
import com.tooba.EduEvent.dto.response.EventResponse;
import com.tooba.EduEvent.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // GET /api/events  — public (permitted in SecurityConfig)
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @ModelAttribute EventRequest request,
            @RequestPart(value = "banner", required = false) MultipartFile banner,
            Authentication authentication) {

        String adminEmail = authentication.getName(); // JWT subject = email
        EventResponse response = eventService.createEvent(request, banner, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT /api/events/{id}  — ADMIN only
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @ModelAttribute EventRequest request,
            @RequestPart(value = "banner", required = false) MultipartFile banner) {

        EventResponse response = eventService.updateEvent(id, request, banner);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/events/{id}  — ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok("Event deleted successfully.");
    }
}