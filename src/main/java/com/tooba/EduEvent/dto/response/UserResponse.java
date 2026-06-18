package com.tooba.EduEvent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String role;
    private String photoUrl;
    // Admin user-management fields
    private LocalDateTime createdAt;
    private Boolean active;
    private Long eventsCount;
}
