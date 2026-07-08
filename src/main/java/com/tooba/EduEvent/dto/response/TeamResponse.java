package com.tooba.EduEvent.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class TeamResponse {
    private String teamId;
    private String teamName;
    private String inviteCode;
    private String leaderId;     // NEW — lets the frontend know who the leader is
    private String leaderName;
    private String hackathonId;  // NEW
    private boolean locked;      // NEW — "Team lock status shown" (feature list)
    private Integer maxSize;     // NEW — render X/maxSize capacity
    private List<MemberDto> members; // now includes PENDING join requests too
}
