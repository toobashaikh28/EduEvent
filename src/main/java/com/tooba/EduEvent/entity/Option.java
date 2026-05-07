package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "options")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_text", nullable = false, length = 500)
    private String optionText;

    @Builder.Default
    @Column(name = "is_correct")
    private Boolean isCorrect = false;
}