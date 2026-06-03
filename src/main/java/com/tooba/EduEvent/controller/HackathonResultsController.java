package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.response.WinnerAnnouncementResponse;
import com.tooba.EduEvent.service.HackathonResultsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hackathon")
@RequiredArgsConstructor
public class HackathonResultsController {

    private final HackathonResultsService hackathonResultsService;

    /**
     * POST /api/hackathon/{id}/announce-winners
     * Admin only. Aggregates scores, saves leaderboard, issues certificates,
     * and notifies all participants.
     */
    @PostMapping("/{id}/announce-winners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WinnerAnnouncementResponse> announceWinners(
            @PathVariable("id") Long hackathonId,
            Authentication authentication) {

        WinnerAnnouncementResponse response =
                hackathonResultsService.announceWinners(hackathonId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/hackathon/{id}/results
     * Public. Returns the ranked leaderboard for a hackathon.
     */
    @GetMapping("/{id}/results")
    public ResponseEntity<List<WinnerAnnouncementResponse.RankedTeam>> getResults(
            @PathVariable("id") Long hackathonId) {

        return ResponseEntity.ok(hackathonResultsService.getResults(hackathonId));
    }
}
