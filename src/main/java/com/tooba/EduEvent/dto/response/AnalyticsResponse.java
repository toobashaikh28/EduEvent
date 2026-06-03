package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyticsResponse {
    private String eventName;
    private Long count;
}