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
    private String status; // REGISTERED or WAITLISTED
    private LocalDateTime registeredAt;
}