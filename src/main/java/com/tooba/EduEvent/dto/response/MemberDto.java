package com.tooba.EduEvent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
    /**
     * The TeamMember record id — THIS is what the accept/reject endpoints need
     * (PUT /api/team/invite/{memberId}/accept). Previously the DTO only exposed
     * the userId, so the frontend had no way to call accept/reject at all.
     */
    private String memberId;
    private String userId;
    private String name;
    private String role;    // LEADER / MEMBER
    private String status;  // ACCEPTED / PENDING
}
