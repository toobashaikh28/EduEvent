package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.EventRequest;
import com.tooba.EduEvent.dto.response.EventResponse;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

import java.util.List;

public interface EventService {
    EventResponse createEvent(EventRequest request, MultipartFile banner, String adminEmail);
    EventResponse updateEvent(String id, EventRequest request, MultipartFile banner, String adminEmail);
    void deleteEvent(String id, String adminEmail);

    /**
     * @param userEmail email of the logged-in user, or null for anonymous visitors.
     *                  Used to fill isRegistered / isWaitlisted per event.
     */
    List<EventResponse> getAllEvents(String type, String status, LocalDateTime date, String userEmail);

    EventResponse getEventById(String id, String userEmail);
}
