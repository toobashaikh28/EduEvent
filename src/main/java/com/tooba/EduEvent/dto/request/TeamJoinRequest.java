package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamJoinRequest {
    @NotBlank(message = "Invite code is required")
    private String inviteCode;
}