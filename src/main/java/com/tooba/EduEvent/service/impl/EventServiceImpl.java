package com.tooba.EduEvent.service.impl;

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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime; 

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final Set<String> JOIN_LINK_TYPES = Set.of("Webinar", "Conference");

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public EventResponse createEvent(EventRequest request, MultipartFile banner, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

        String bannerPath = saveBanner(banner);
        String joinLink = resolveJoinLink(request);

        Event event = Event.builder()
                .title(request.getTitle())
                .type(request.getType())
                .description(request.getDescription())
                .capacity(request.getCapacity() != null ? request.getCapacity() : 100)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(request.getStatus() != null ? request.getStatus() : "UPCOMING")
                .bannerUrl(bannerPath)
                .joinLink(joinLink)
                .admin(admin)
                .build();

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Override
    public EventResponse updateEvent(Long id, EventRequest request, MultipartFile banner) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found with id: " + id));

        event.setTitle(request.getTitle());
        event.setType(request.getType());
        event.setDescription(request.getDescription());
        if (request.getCapacity() != null) event.setCapacity(request.getCapacity());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        if (request.getStatus() != null) event.setStatus(request.getStatus());
        event.setJoinLink(resolveJoinLink(request));

        String bannerPath = saveBanner(banner);
        if (bannerPath != null) event.setBannerUrl(bannerPath);

        return toResponse(eventRepository.save(event));
    }

    @Override
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    @Override
    public List<EventResponse> getAllEvents(String type, String status, LocalDateTime date) {
        List<Event> events;
        
        if (type != null) {
            events = eventRepository.findByType(type);
        } else if (status != null) {
            events = eventRepository.findByStatus(status);
        } else if (date != null) {
            events = eventRepository.findByStartTimeAfter(date);
        } else {
            events = eventRepository.findAll();
        }
        
        return events.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public EventResponse getEventById(Long id) {
        // Throwing an exception here ensures we don't return null
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found with id: " + id));
        return toResponse(event);
    }

    private String saveBanner(MultipartFile banner) {
        if (banner == null || banner.isEmpty()) return null;
        try {
            // Absolute path fix for Windows — avoids Tomcat temp dir issue
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "banners");
            Files.createDirectories(uploadDir);

            String originalName = banner.getOriginalFilename();
            String extension = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf(".")) : "";
            String uniqueName = UUID.randomUUID() + extension;

            Path targetPath = uploadDir.resolve(uniqueName);
            Files.copy(banner.getInputStream(), targetPath); // works on Windows

            return "uploads/banners/" + uniqueName;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save banner image: " + e.getMessage());
        }
    }

    private String resolveJoinLink(EventRequest request) {
        if (request.getType() != null && JOIN_LINK_TYPES.contains(request.getType())) {
            return request.getJoinLink();
        }
        return null;
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
                .adminName(event.getAdmin() != null ? event.getAdmin().getName() : null)
                .createdAt(event.getCreatedAt())
                .build();
    }
}