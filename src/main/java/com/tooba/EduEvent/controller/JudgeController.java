package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.request.ScoreRequest;
import com.tooba.EduEvent.dto.response.ScoreResponse;
import com.tooba.EduEvent.dto.response.SubmissionResponse;
import com.tooba.EduEvent.service.JudgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeService judgeService;

    // POST /api/hackathon/{id}/assign-judge?judgeUserId=X
    @PostMapping("/api/hackathon/{id}/assign-judge")
    public ResponseEntity<String> assignJudge(
            @PathVariable("id") Long hackathonId,
            @RequestParam("judgeUserId") Long judgeUserId,
            Authentication authentication) {

        judgeService.assignJudge(hackathonId, judgeUserId, authentication.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Judge assigned successfully.");
    }

    // GET /api/judge/hackathon/{id}/submissions
    @GetMapping("/api/judge/hackathon/{id}/submissions")
    public ResponseEntity<List<SubmissionResponse>> getSubmissions(
            @PathVariable("id") Long hackathonId,
            Authentication authentication) {

        return ResponseEntity.ok(
                judgeService.getSubmissionsForJudge(hackathonId, authentication.getName()));
    }

    // POST /api/judge/score
    @PostMapping("/api/judge/score")
    public ResponseEntity<ScoreResponse> score(
            @Valid @RequestBody ScoreRequest request,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(judgeService.scoreSubmission(request, authentication.getName()));
    }

    // GET /api/judge/hackathon/{id}/my-scores
    @GetMapping("/api/judge/hackathon/{id}/my-scores")
    public ResponseEntity<List<ScoreResponse>> getMyScores(
            @PathVariable("id") Long hackathonId,
            Authentication authentication) {

        return ResponseEntity.ok(
                judgeService.getMyScores(hackathonId, authentication.getName()));
    }
}