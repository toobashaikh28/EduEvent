package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CertificateResponse {
    private String id;
    private String participantName;
    private String eventTitle;
    private UUID certUuid;
    private String verifyUrl;
    private LocalDateTime issuedAt;
}