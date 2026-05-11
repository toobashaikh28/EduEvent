package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "Name cannot be empty")
    private String name;
    private String photoUrl;
}