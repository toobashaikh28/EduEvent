package com.tooba.EduEvent.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CertificateResponse {
    private String certificateId;
    private String userName;
    private String eventTitle;
    private LocalDate issueDate;
    private String downloadUrl;
}