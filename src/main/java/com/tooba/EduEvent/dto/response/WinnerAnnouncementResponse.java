package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class WinnerAnnouncementResponse {
    private Long hackathonId;
    private String hackathonTitle;
    private int totalTeamsRanked;
    private List<RankedTeam> rankings;

    @Data
    @Builder
    public static class RankedTeam {
        private int rank;
        private Long teamId;
        private String teamName;
        private String leaderName;
        private List<String> memberNames;
        private BigDecimal totalScore;
        private boolean certificatesIssued;
    }
}
