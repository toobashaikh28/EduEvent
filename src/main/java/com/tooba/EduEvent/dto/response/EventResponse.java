package com.tooba.EduEvent.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventResponse {
    private String id;
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

    // Alias the frontend uses on event cards ("org: e.createdBy")
    private String createdBy;

    private LocalDateTime createdAt;

    // ── NEW: live registration state (fixes "Registered" persisting on reload) ──
    /** Number of CONFIRMED (REGISTERED) seats taken — drives the capacity bar. */
    private long registeredCount;

    /** True if the currently logged-in user has a REGISTERED seat for this event. */
    @JsonProperty("isRegistered")
    private boolean isRegistered;

    /** True if the currently logged-in user is on the waitlist for this event. */
    @JsonProperty("isWaitlisted")
    private boolean isWaitlisted;
}
