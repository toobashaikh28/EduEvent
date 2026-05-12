package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.EventRequest;
import com.tooba.EduEvent.dto.response.EventResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EventService {
    EventResponse createEvent(EventRequest request, MultipartFile banner, String adminEmail);
    EventResponse updateEvent(Long id, EventRequest request, MultipartFile banner);
    void deleteEvent(Long id);
    List<EventResponse> getAllEvents();
}