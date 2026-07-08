package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationResponse {
    private String id;
    private String eventId;
    private String eventTitle;
    private String userId;
    private String userName;
    private String status; // REGISTERED, WAITLISTED or CANCELLED
    private LocalDateTime registeredAt;

    /**
     * FIX (feature list): "Position number assigned to each waitlisted user".
     * 1-based position in the waitlist queue; null for non-waitlisted rows.
     */
    private Integer waitlistPosition;
}
