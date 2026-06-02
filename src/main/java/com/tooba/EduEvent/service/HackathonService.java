package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.LeaderboardResponse;
import java.util.List;

public interface HackathonService {
    void announceWinners(Long hackathonId);
    List<LeaderboardResponse> getResults(Long hackathonId);
}
