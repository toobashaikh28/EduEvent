package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.request.EventRequest;
import com.tooba.EduEvent.dto.response.EventResponse;
import com.tooba.EduEvent.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // GET /api/events — Public discovery with optional filters.
    // The endpoint stays public, but IF a JWT is sent, the JwtAuthenticationFilter
    // still populates Authentication — so we can tell the frontend whether THIS
    // user is already registered (isRegistered) and how many seats are taken.
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            Authentication authentication) {

        String userEmail = (authentication != null) ? authentication.getName() : null;
        return ResponseEntity.ok(eventService.getAllEvents(type, status, date, userEmail));
    }

    // GET /api/events/{id} — Public, returns a single event or clean 404
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable String id,
            Authentication authentication) {

        String userEmail = (authentication != null) ? authentication.getName() : null;
        return ResponseEntity.ok(eventService.getEventById(id, userEmail));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @ModelAttribute EventRequest request,
            @RequestPart(value = "banner", required = false) MultipartFile banner,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        EventResponse response = eventService.createEvent(request, banner, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable String id,
            @Valid @ModelAttribute EventRequest request,
            @RequestPart(value = "banner", required = false) MultipartFile banner) {

        EventResponse response = eventService.updateEvent(id, request, banner);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteEvent(@PathVariable String id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok("Event deleted successfully.");
    }
}
