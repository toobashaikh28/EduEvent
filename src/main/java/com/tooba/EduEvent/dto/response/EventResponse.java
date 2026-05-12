package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private String type;
    private String description;
    private Integer capacity;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String bannerUrl;
    private String joinLink;   // only populated for Webinar / Conference
    private String adminName;  // name of the admin who created the event
    private LocalDateTime createdAt;
}