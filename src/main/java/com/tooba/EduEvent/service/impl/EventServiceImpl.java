package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.chain.EventHandler;
import com.tooba.EduEvent.chain.handlers.AuthorizationHandler;
import com.tooba.EduEvent.chain.handlers.DuplicateCheckHandler;
import com.tooba.EduEvent.chain.handlers.EventValidationHandler;
import com.tooba.EduEvent.dto.request.EventRequest;
import com.tooba.EduEvent.dto.response.EventResponse;
import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.EventRepository;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final Set<String> JOIN_LINK_TYPES = Set.of("WEBINAR", "CONFERENCE");

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // Chain of Responsibility handlers
    private final AuthorizationHandler authorizationHandler;
    private final EventValidationHandler validationHandler;
    private final DuplicateCheckHandler duplicateCheckHandler;

    /**
     * Builds the Chain of Responsibility:
     * Auth → Validation → DuplicateCheck
     * Returns the head of the chain.
     */
    private EventHandler buildChain() {
        authorizationHandler
            .setNext(validationHandler)
            .setNext(duplicateCheckHandler);
        return authorizationHandler;
    }

    @Override
    public List<EventResponse> getAllEvents(String type, String status, LocalDateTime date) {
        List<Event> events;

        if (type != null && !type.isBlank() && status != null && !status.isBlank()) {
            events = eventRepository.findByTypeIgnoreCase(type.trim()).stream()
                    .filter(e -> e.getStatus() != null &&
                            e.getStatus().equalsIgnoreCase(status.trim()))
                    .collect(Collectors.toList());
        } else if (type != null && !type.isBlank()) {
            events = eventRepository.findByTypeIgnoreCase(type.trim());
        } else if (status != null && !status.isBlank()) {
            events = eventRepository.findByStatusIgnoreCase(status.trim());
        } else {
            events = eventRepository.findAll();
        }

        if (date != null) {
            events = events.stream()
                    .filter(e -> e.getStartTime() != null && e.getStartTime().isAfter(date))
                    .collect(Collectors.toList());
        }

        return events.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public EventResponse getEventById(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found with id: " + id));
        return toResponse(event);
    }

    @Override
    public EventResponse createEvent(EventRequest request, MultipartFile banner, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Admin not found"));

        // ── CHAIN OF RESPONSIBILITY ───────────────────────────────────────────
        // Auth → Validation → DuplicateCheck (any handler can reject and stop)
        buildChain().handle(request, admin, "CREATE");
        // ─────────────────────────────────────────────────────────────────────

        String bannerPath = saveBanner(banner);
        String normalizedType = normalizeType(request.getType());
        String joinLink = resolveJoinLink(request);

        Event event = Event.builder()
                .title(request.getTitle())
                .type(normalizedType)
                .description(request.getDescription())
                .capacity(request.getCapacity() != null ? request.getCapacity() : 100)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(request.getStatus() != null ? request.getStatus().toUpperCase() : "UPCOMING")
                .bannerUrl(bannerPath)
                .joinLink(joinLink)
                .adminId(admin.getId())
                .build();

        return toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse updateEvent(String id, EventRequest request, MultipartFile banner) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found with id: " + id));

        // ── CHAIN OF RESPONSIBILITY ───────────────────────────────────────────
        // Reuse Auth + Validation for UPDATE (skip duplicate check — title can stay same)
        User admin = userRepository.findById(event.getAdminId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Event owner not found"));
        authorizationHandler.setNext(validationHandler);
        authorizationHandler.handle(request, admin, "UPDATE");
        // ─────────────────────────────────────────────────────────────────────

        event.setTitle(request.getTitle());
        event.setType(normalizeType(request.getType()));
        event.setDescription(request.getDescription());
        if (request.getCapacity() != null) event.setCapacity(request.getCapacity());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        if (request.getStatus() != null) event.setStatus(request.getStatus().toUpperCase());
        event.setJoinLink(resolveJoinLink(request));

        String bannerPath = saveBanner(banner);
        if (bannerPath != null) event.setBannerUrl(bannerPath);

        return toResponse(eventRepository.save(event));
    }

    @Override
    public void deleteEvent(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found with id: " + id));

        // ── CHAIN OF RESPONSIBILITY ───────────────────────────────────────────
        // Auth check only for DELETE
        User admin = userRepository.findById(event.getAdminId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Event owner not found"));
        authorizationHandler.setNext(null);
        authorizationHandler.handle(new EventRequest(), admin, "DELETE");
        // ─────────────────────────────────────────────────────────────────────

        eventRepository.deleteById(id);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) return type;
        String t = type.trim();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1).toLowerCase();
    }

    private String resolveJoinLink(EventRequest request) {
        if (request.getType() != null &&
                JOIN_LINK_TYPES.contains(request.getType().toUpperCase())) {
            return request.getJoinLink();
        }
        return null;
    }

    private String saveBanner(MultipartFile banner) {
        if (banner == null || banner.isEmpty()) return null;
        try {
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "banners");
            Files.createDirectories(uploadDir);
            String originalName = banner.getOriginalFilename();
            String extension = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf(".")) : "";
            String uniqueName = UUID.randomUUID() + extension;
            Files.copy(banner.getInputStream(), uploadDir.resolve(uniqueName));
            return "uploads/banners/" + uniqueName;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save banner image: " + e.getMessage());
        }
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .type(event.getType())
                .description(event.getDescription())
                .capacity(event.getCapacity())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .bannerUrl(event.getBannerUrl())
                .joinLink(event.getJoinLink())
                .adminName(event.getAdminId() != null
                        ? userRepository.findById(event.getAdminId()).map(User::getName).orElse(null)
                        : null)
                .createdAt(event.getCreatedAt())
                .build();
    }
}
