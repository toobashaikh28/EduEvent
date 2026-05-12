package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.Future;
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

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String status; // UPCOMING, LIVE, COMPLETED, CANCELLED

    /**
     * Only used for Webinar and Conference types.
     * Stored in event.joinLink.
     */
    private String joinLink;
}