package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Certificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Builder.Default
    private UUID certUuid = UUID.randomUUID();

    private String pdfUrl;
    
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();
}