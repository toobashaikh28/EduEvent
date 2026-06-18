package com.tooba.EduEvent.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class SubmitQuizRequest {
    // key = questionId, value = selectedOptionId
    private Map<String, String> answers;
}