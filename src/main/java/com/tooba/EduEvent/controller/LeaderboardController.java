package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.response.LeaderboardResponse;
import com.tooba.EduEvent.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/event/{id}")
    public ResponseEntity<List<LeaderboardResponse>> getEventLeaderboard(@PathVariable String id) {
        return ResponseEntity.ok(leaderboardService.getEventLeaderboard(id));
    }

    @GetMapping("/hackathon/{id}")
    public ResponseEntity<List<LeaderboardResponse>> getHackathonLeaderboard(@PathVariable String id) {
        return ResponseEntity.ok(leaderboardService.getHackathonLeaderboard(id));
    }

    @GetMapping("/global")
    public ResponseEntity<List<LeaderboardResponse>> getGlobalLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getGlobalLeaderboard());
    }
}