package com.tooba.EduEvent.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** A hackathon a judge is assigned to — used to populate the judge's pickers. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JudgeHackathonResponse {
    private String id;
    private String title;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
