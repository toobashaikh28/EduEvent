package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "scores")
@CompoundIndex(name = "uq_judge_submission", def = "{'judgeId': 1, 'submissionId': 1}", unique = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Score {

    @Id
    private String id;

    private String judgeId;

    private String submissionId;

    private Integer scoreValue;

    private String feedback;

    @CreatedDate
    private LocalDateTime scoredAt;
}
