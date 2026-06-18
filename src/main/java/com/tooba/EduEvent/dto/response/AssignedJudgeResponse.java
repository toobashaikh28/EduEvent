package com.tooba.EduEvent.dto.response;

import lombok.*;

/** A judge currently assigned to an event — used by the admin assign panel. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssignedJudgeResponse {
    private String id;       // user id
    private String name;
    private String email;
}
