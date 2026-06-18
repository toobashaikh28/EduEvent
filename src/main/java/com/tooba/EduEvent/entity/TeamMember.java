package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "team_members")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TeamMember {
    @Id
    private String id;

    private String teamId;

    private String userId;

    @Builder.Default
    private String role = "MEMBER"; // 'LEADER', 'MEMBER'

    @Builder.Default
    private String status = "PENDING"; // 'PENDING', 'ACCEPTED'
}
