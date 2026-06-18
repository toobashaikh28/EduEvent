package com.tooba.EduEvent.dto.response;

import lombok.*;

/** A quiz as seen by a participant on the "My Quizzes" page (real data). */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserQuizResponse {
    private String id;
    private String title;          // event title
    private String eventName;
    private Integer durationMinutes;
    private Integer questionCount;
    private Integer passMark;       // percentage
    private String status;          // active | upcoming | completed
    private boolean attempted;
    private Integer score;          // best score % (null if not attempted)
    private boolean passed;
    private String attemptDate;     // e.g. "Jun 3" (null if not attempted)
}
