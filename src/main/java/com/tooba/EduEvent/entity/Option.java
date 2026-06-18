package com.tooba.EduEvent.entity;

import lombok.*;

/**
 * Embedded document — options live inside a {@link Question} document
 * (not a separate collection). The {@code id} is assigned in the service
 * (a UUID string) so answers can reference a stable option id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Option {

    private String id;

    private String optionText;

    @Builder.Default
    private Boolean isCorrect = false;
}
