package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "judge_assignments")
@CompoundIndex(name = "uq_judge_hackathon", def = "{'judgeId': 1, 'hackathonId': 1}", unique = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JudgeAssignment {

    @Id
    private String id;

    private String judgeId;

    private String hackathonId;

    @CreatedDate
    private LocalDateTime assignedAt;
}
