package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ViolationResponse {
    private String violationId;
    private Integer totalCount;
    private String sessionStatus; // so frontend knows if INVALIDATED
}