package com.tooba.EduEvent.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;
    private String organizerName; // Useful for the frontend to show who is hosting
}