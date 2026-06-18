package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.WinnerAnnouncementResponse;

import java.util.List;

public interface HackathonResultsService {

    /** Admin: aggregate scores, save leaderboard, issue certs, notify all participants */
    WinnerAnnouncementResponse announceWinners(String hackathonId, String adminEmail);

    /** Public: return ranked results for a hackathon */
    List<WinnerAnnouncementResponse.RankedTeam> getResults(String hackathonId);
}
