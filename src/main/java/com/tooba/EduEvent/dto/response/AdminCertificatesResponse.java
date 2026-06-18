package com.tooba.EduEvent.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin Certificates page payload — stat cards plus the full list, all real data.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminCertificatesResponse {

    private long totalIssued;
    private long thisMonth;
    private long pendingGeneration;   // registered-for-completed-event but no cert yet

    private List<Row> certificates;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Row {
        private String id;             // Mongo id (used for download/delete)
        private String certNumber;     // human-friendly e.g. CERT-A1B2C3D4
        private String recipient;      // User.name
        private String eventTitle;     // Event.title
        private String eventType;      // lowercase: hackathon/quiz/webinar/conference/other
        private LocalDateTime issuedAt;
    }
}
