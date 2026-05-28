package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OptionResponse {
    private Long id;
    private String optionText;
    private Boolean isCorrect; // null when sent to user during quiz, populated for admin
}