package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    private String id;

    private String quizId;

    private String questionText;

    // Options are embedded inside the question document.
    @Builder.Default
    private List<Option> options = new ArrayList<>();
}
