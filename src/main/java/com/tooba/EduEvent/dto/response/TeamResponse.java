package com.tooba.EduEvent.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class TeamResponse {
    private String teamId;
    private String teamName;
    private String inviteCode;
    private String leaderName;
    private List<MemberDto> members; // Updated this line!
}