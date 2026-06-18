package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quizzes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {
    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    @Builder.Default
    private Integer durationMinutes = 30;

    @Builder.Default
    private java.math.BigDecimal passScore = new java.math.BigDecimal("50.00");

    @Builder.Default
    private Boolean randomize = true;
}
