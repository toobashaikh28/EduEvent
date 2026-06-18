package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.request.ScoreRequest;
import com.tooba.EduEvent.dto.response.AssignedJudgeResponse;
import com.tooba.EduEvent.dto.response.JudgeHackathonResponse;
import com.tooba.EduEvent.dto.response.ScoreResponse;
import com.tooba.EduEvent.dto.response.SubmissionResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.JudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeServiceImpl implements JudgeService {

    private final UserRepository             userRepository;
    private final EventRepository            eventRepository;
    private final TeamRepository             teamRepository;
    private final JudgeAssignmentRepository  judgeAssignmentRepository;
    private final SubmissionRepository       submissionRepository;
    private final ScoreRepository            scoreRepository;

    // ── POST /api/hackathon/{id}/assign-judge ────────────────────────────────
    @Override
    public void assignJudge(String hackathonId, String judgeUserId, String adminEmail) {
        User admin = resolveUser(adminEmail);
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can assign judges.");
        }

        Event hackathon = resolveHackathon(hackathonId);

        User judge = userRepository.findById(judgeUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + judgeUserId));
        if (!"JUDGE".equalsIgnoreCase(judge.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User " + judgeUserId + " does not have the JUDGE role.");
        }

        if (judgeAssignmentRepository.existsByJudgeIdAndHackathonId(judgeUserId, hackathonId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Judge is already assigned to this hackathon.");
        }

        JudgeAssignment assignment = JudgeAssignment.builder()
                .judgeId(judge.getId())
                .hackathonId(hackathon.getId())
                .build();

        judgeAssignmentRepository.save(assignment);
        log.info("Judge {} assigned to hackathon {} by admin {}", judgeUserId, hackathonId, adminEmail);
    }

    // ── GET /api/hackathon/{id}/judges ───────────────────────────────────────
    @Override
    public List<AssignedJudgeResponse> getAssignedJudges(String hackathonId, String adminEmail) {
        User admin = resolveUser(adminEmail);
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can view assigned judges.");
        }
        return judgeAssignmentRepository.findByHackathonId(hackathonId).stream()
                .map(a -> userRepository.findById(a.getJudgeId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(u -> AssignedJudgeResponse.builder()
                        .id(u.getId()).name(u.getName()).email(u.getEmail()).build())
                .toList();
    }

    // ── DELETE /api/hackathon/{id}/judges/{judgeUserId} ──────────────────────
    @Override
    public void removeJudge(String hackathonId, String judgeUserId, String adminEmail) {
        User admin = resolveUser(adminEmail);
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can remove judges.");
        }
        JudgeAssignment assignment = judgeAssignmentRepository
                .findByJudgeIdAndHackathonId(judgeUserId, hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "That judge is not assigned to this event."));
        judgeAssignmentRepository.delete(assignment);
        log.info("Judge {} unassigned from hackathon {} by admin {}", judgeUserId, hackathonId, adminEmail);
    }

    // ── GET /api/judge/my-hackathons ─────────────────────────────────────────
    @Override
    public List<JudgeHackathonResponse> getMyHackathons(String judgeEmail) {
        User judge = resolveUser(judgeEmail);
        return judgeAssignmentRepository.findByJudgeId(judge.getId()).stream()
                .map(a -> eventRepository.findById(a.getHackathonId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(ev -> JudgeHackathonResponse.builder()
                        .id(ev.getId())
                        .title(ev.getTitle())
                        .status(ev.getStatus())
                        .startTime(ev.getStartTime())
                        .endTime(ev.getEndTime())
                        .build())
                .toList();
    }

    // ── GET /api/judge/hackathon/{id}/submissions ────────────────────────────
    @Override
    public List<SubmissionResponse> getSubmissionsForJudge(String hackathonId, String judgeEmail) {
        User judge = resolveUser(judgeEmail);
        verifyJudgeAssigned(judge.getId(), hackathonId);

        return submissionRepository.findAllByHackathonId(hackathonId).stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    // ── POST /api/judge/score ────────────────────────────────────────────────
    @Override
    public ScoreResponse scoreSubmission(ScoreRequest request, String judgeEmail) {
        User judge = resolveUser(judgeEmail);

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Submission not found: " + request.getSubmissionId()));

        verifyJudgeAssigned(judge.getId(), submission.getHackathonId());

        if (scoreRepository.existsByJudgeIdAndSubmissionId(judge.getId(), submission.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already scored this submission.");
        }

        Score score = Score.builder()
                .judgeId(judge.getId())
                .submissionId(submission.getId())
                .scoreValue(request.getScoreValue())
                .feedback(request.getFeedback())
                .build();

        Score saved = scoreRepository.save(score);
        log.info("Score saved: judge={} submission={} value={}", judge.getId(), submission.getId(), request.getScoreValue());
        return toScoreResponse(saved);
    }

    // ── GET /api/judge/hackathon/{id}/my-scores ──────────────────────────────
    @Override
    public List<ScoreResponse> getMyScores(String hackathonId, String judgeEmail) {
        User judge = resolveUser(judgeEmail);
        verifyJudgeAssigned(judge.getId(), hackathonId);

        List<String> submissionIds = submissionRepository.findAllByHackathonId(hackathonId)
                .stream().map(Submission::getId).toList();
        if (submissionIds.isEmpty()) return List.of();

        return scoreRepository
                .findByJudgeIdAndSubmissionIdIn(judge.getId(), submissionIds)
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

    private Event resolveHackathon(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Hackathon not found: " + id));
    }

    private void verifyJudgeAssigned(String judgeId, String hackathonId) {
        if (!judgeAssignmentRepository.existsByJudgeIdAndHackathonId(judgeId, hackathonId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not assigned as a judge for this hackathon.");
        }
    }

    private String teamName(String teamId) {
        return teamRepository.findById(teamId).map(Team::getName).orElse("Unknown Team");
    }

    private String eventTitle(String eventId) {
        return eventRepository.findById(eventId).map(Event::getTitle).orElse("Unknown Event");
    }

    private SubmissionResponse toSubmissionResponse(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .teamId(s.getTeamId())
                .teamName(teamName(s.getTeamId()))
                .hackathonId(s.getHackathonId())
                .hackathonTitle(eventTitle(s.getHackathonId()))
                .title(s.getTitle())
                .description(s.getDescription())
                .githubUrl(s.getGithubUrl())
                .filePath(s.getFilePath())
                .submittedAt(s.getSubmittedAt())
                .build();
    }

    private ScoreResponse toScoreResponse(Score s) {
        String teamNm = submissionRepository.findById(s.getSubmissionId())
                .map(sub -> teamName(sub.getTeamId())).orElse("Unknown Team");
        String judgeNm = userRepository.findById(s.getJudgeId()).map(User::getName).orElse("Unknown");
        return ScoreResponse.builder()
                .id(s.getId())
                .submissionId(s.getSubmissionId())
                .teamName(teamNm)
                .judgeId(s.getJudgeId())
                .judgeName(judgeNm)
                .scoreValue(s.getScoreValue())
                .feedback(s.getFeedback())
                .scoredAt(s.getScoredAt())
                .build();
    }
}
