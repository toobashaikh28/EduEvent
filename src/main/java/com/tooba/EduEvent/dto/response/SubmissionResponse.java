package com.tooba.EduEvent.dto.response;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
 
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionResponse {
    private Long id;
    private Long teamId;
    private String teamName;
    private Long eventId;
    private String eventTitle;
    private String fileUrl;
    private String notes;
    private Integer score;
    private LocalDateTime submittedAt;
}
 