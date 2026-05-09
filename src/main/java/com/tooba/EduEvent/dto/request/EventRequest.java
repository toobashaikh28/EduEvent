package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Future;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequest {
    @NotBlank(message = "Event title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    @NotBlank(message = "Location/Link is required")
    private String location;
}