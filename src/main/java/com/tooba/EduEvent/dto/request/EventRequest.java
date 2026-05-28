package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {

    @NotBlank(message = "Event title is required")
    private String title;

    @NotBlank(message = "Event type is required (Webinar, Conference, Hackathon, Quiz)")
    private String type;

    private String description;

    private Integer capacity;

    // Fix: removed @Future — update operations need to set startTime to existing
    // (possibly past) values without triggering a 400 validation error.
    // Validate future start time in the service layer on CREATE only if needed.
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String status; // UPCOMING, LIVE, COMPLETED, CANCELLED

    /** Only used for Webinar and Conference event types. */
    private String joinLink;
}
