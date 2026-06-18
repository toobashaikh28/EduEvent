package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuestionResponse {
    private String id;
    private String text;
    private List<OptionResponse> options;
}