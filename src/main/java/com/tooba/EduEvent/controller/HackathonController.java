package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.response.LeaderboardResponse;
import com.tooba.EduEvent.service.HackathonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hackathon")
@RequiredArgsConstructor
public class HackathonController {

    private final HackathonService hackathonService;

    @PostMapping("/{id}/announce-winners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> announceWinners(@PathVariable Long id) {
        hackathonService.announceWinners(id);
        return ResponseEntity.ok("Winners announced successfully.");
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<List<LeaderboardResponse>> getResults(@PathVariable Long id) {
        return ResponseEntity.ok(hackathonService.getResults(id));
    }
}
