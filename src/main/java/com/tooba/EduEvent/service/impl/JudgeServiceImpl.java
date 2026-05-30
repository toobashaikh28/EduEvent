package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.request.ScoreRequest;
import com.tooba.EduEvent.dto.response.ScoreResponse;
import com.tooba.EduEvent.dto.response.SubmissionResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.JudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeServiceImpl implements JudgeService {

    private final UserRepository             userRepository;
    private final EventRepository            eventRepository;
    private final JudgeAssignmentRepository  judgeAssignmentRepository;
    private final SubmissionRepository       submissionRepository;
    private final ScoreRepository            scoreRepository;

    // ── POST /api/hackathon/{id}/assign-judge ────────────────────────────────
    @Override
    @Transactional
    public void assignJudge(Long hackathonId, Long judgeUserId, String adminEmail) {
        // Verify caller is an ADMIN
        User admin = resolveUser(adminEmail);
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only admins can assign judges.");
        }

        Event hackathon = resolveHackathon(hackathonId);

        // Verify target user exists and has JUDGE role
        User judge = userRepository.findById(judgeUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + judgeUserId));
        if (!"JUDGE".equalsIgnoreCase(judge.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User " + judgeUserId + " does not have the JUDGE role.");
        }

        // Prevent duplicate assignment
        if (judgeAssignmentRepository.existsByJudgeIdAndHackathonId(judgeUserId, hackathonId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Judge is already assigned to this hackathon.");
        }

        JudgeAssignment assignment = JudgeAssignment.builder()
                .judge(judge)
                .hackathon(hackathon)
                .build();

        judgeAssignmentRepository.save(assignment);
        log.info("Judge {} assigned to hackathon {} by admin {}", judgeUserId, hackathonId, adminEmail);
    }

    // ── GET /api/judge/hackathon/{id}/submissions ────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsForJudge(Long hackathonId, String judgeEmail) {
        User judge = resolveUser(judgeEmail);
        verifyJudgeAssigned(judge.getId(), hackathonId);

        return submissionRepository.findAllByHackathonId(hackathonId).stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    // ── POST /api/judge/score ────────────────────────────────────────────────
    @Override
    @Transactional
    public ScoreResponse scoreSubmission(ScoreRequest request, String judgeEmail) {
        User judge = resolveUser(judgeEmail);

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Submission not found: " + request.getSubmissionId()));

        // Validate judge is assigned to this submission's hackathon
        verifyJudgeAssigned(judge.getId(), submission.getHackathon().getId());

        // Prevent duplicate scoring
        if (scoreRepository.existsByJudgeIdAndSubmissionId(judge.getId(), submission.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already scored this submission.");
        }

        Score score = Score.builder()
                .judge(judge)
                .submission(submission)
                .scoreValue(request.getScoreValue())
                .feedback(request.getFeedback())
                .build();

        Score saved = scoreRepository.save(score);
        log.info("Score saved: judge={} submission={} value={}", judge.getId(), submission.getId(), request.getScoreValue());
        return toScoreResponse(saved);
    }

    // ── GET /api/judge/hackathon/{id}/my-scores ──────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> getMyScores(Long hackathonId, String judgeEmail) {
        User judge = resolveUser(judgeEmail);
        verifyJudgeAssigned(judge.getId(), hackathonId);

        return scoreRepository
                .findAllByJudgeIdAndSubmissionHackathonId(judge.getId(), hackathonId)
                .stream()
                .map(this::toScoreResponse)
                .toList();
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
    }

    private Event resolveHackathon(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Hackathon not found: " + id));
    }

    /** Throws 403 if the judge is NOT assigned to the given hackathon. */
    private void verifyJudgeAssigned(Long judgeId, Long hackathonId) {
        if (!judgeAssignmentRepository.existsByJudgeIdAndHackathonId(judgeId, hackathonId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not assigned as a judge for this hackathon.");
        }
    }

    private SubmissionResponse toSubmissionResponse(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .teamId(s.getTeam().getId())
                .teamName(s.getTeam().getName())
                .hackathonId(s.getHackathon().getId())
                .hackathonTitle(s.getHackathon().getTitle())
                .title(s.getTitle())
                .description(s.getDescription())
                .githubUrl(s.getGithubUrl())
                .filePath(s.getFilePath())
                .submittedAt(s.getSubmittedAt())
                .build();
    }

    private ScoreResponse toScoreResponse(Score s) {
        return ScoreResponse.builder()
                .id(s.getId())
                .submissionId(s.getSubmission().getId())
                .teamName(s.getSubmission().getTeam().getName())
                .judgeId(s.getJudge().getId())
                .judgeName(s.getJudge().getName())
                .scoreValue(s.getScoreValue())
                .feedback(s.getFeedback())
                .scoredAt(s.getScoredAt())
                .build();
    }
}