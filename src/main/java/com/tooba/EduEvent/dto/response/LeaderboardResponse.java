package com.tooba.EduEvent.dto.response;

import lombok.Data;

@Data
public class LeaderboardResponse {
    private int rank;
    private String teamOrUserName;
    private int totalPoints;
}