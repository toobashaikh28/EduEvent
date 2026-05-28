package com.tooba.EduEvent.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {

    @NotBlank(message = "Question text is required")
    private String text;

    @NotEmpty(message = "At least one option is required")
    @Valid
    private List<OptionDto> options;

    @Data
    public static class OptionDto {
        @NotBlank(message = "Option text is required")
        private String optionText;

        private boolean isCorrect;
    }
}
