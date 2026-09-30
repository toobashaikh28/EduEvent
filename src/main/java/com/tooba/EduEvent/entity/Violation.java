package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "violations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Violation {

    @Id
    private String id;
    @org.springframework.data.mongodb.core.index.Indexed

    private String sessionId;

    // face_missing, gaze_away, tab_switch
    private String type;

    @CreatedDate
    private LocalDateTime recordedAt;
}
