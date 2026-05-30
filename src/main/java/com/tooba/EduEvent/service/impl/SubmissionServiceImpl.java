package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.request.SubmissionRequest;
import com.tooba.EduEvent.dto.response.SubmissionResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository  submissionRepository;
    private final EventRepository       eventRepository;
    private final UserRepository        userRepository;
    private final TeamMemberRepository  teamMemberRepository;

    // ── POST /api/hackathon/{id}/submit ──────────────────────────────────────
    @Override
    @Transactional
    public SubmissionResponse submit(Long hackathonId, String userEmail,
                                     SubmissionRequest request, MultipartFile file) {
        Event hackathon = resolveHackathon(hackathonId);
        User  user      = resolveUser(userEmail);
        Team  team      = resolveTeam(user, hackathonId);

        // Validation: team must be locked
        if (!Boolean.TRUE.equals(team.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your team must be locked before submitting.");
        }

        // Validation: submission deadline (hackathon endTime) must not have passed
        if (LocalDateTime.now().isAfter(hackathon.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Submission deadline has passed (ended: " + hackathon.getEndTime() + ").");
        }

        // Validation: no duplicate — one submission per team per hackathon
        if (submissionRepository.findByTeamIdAndHackathonId(team.getId(), hackathonId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Your team already has a submission. Use PUT to edit it.");
        }

        String filePath = saveFile(file, team.getId());

        Submission submission = Submission.builder()
                .team(team)
                .hackathon(hackathon)
                .title(request.getTitle())
                .description(request.getDescription())
                .githubUrl(request.getGithubUrl())
                .filePath(filePath)
                .build();

        Submission saved = submissionRepository.save(submission);
        log.info("Submission created: id={} team={} hackathon={}", saved.getId(), team.getId(), hackathonId);
        return toResponse(saved);
    }

    // ── PUT /api/hackathon/{id}/submit ───────────────────────────────────────
    @Override
    @Transactional
    public SubmissionResponse editSubmission(Long hackathonId, String userEmail,
                                             SubmissionRequest request, MultipartFile file) {
        Event hackathon = resolveHackathon(hackathonId);
        User  user      = resolveUser(userEmail);
        Team  team      = resolveTeam(user, hackathonId);

        if (LocalDateTime.now().isAfter(hackathon.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Submission deadline has passed. Edits are no longer accepted.");
        }

        Submission submission = submissionRepository
                .findByTeamIdAndHackathonId(team.getId(), hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No submission found for your team. POST first."));

        submission.setTitle(request.getTitle());
        submission.setDescription(request.getDescription());
        submission.setGithubUrl(request.getGithubUrl());

        // Replace file only when a new one is provided
        if (file != null && !file.isEmpty()) {
            submission.setFilePath(saveFile(file, team.getId()));
        }

        Submission saved = submissionRepository.save(submission);
        log.info("Submission updated: id={} team={} hackathon={}", saved.getId(), team.getId(), hackathonId);
        return toResponse(saved);
    }

    // ── GET /api/hackathon/{id}/my-submission ────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmission(Long hackathonId, String userEmail) {
        User user = resolveUser(userEmail);
        Team team = resolveTeam(user, hackathonId);

        return submissionRepository
                .findByTeamIdAndHackathonId(team.getId(), hackathonId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Your team has not submitted yet."));
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Event resolveHackathon(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Hackathon not found: " + id));
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
    }

    private Team resolveTeam(User user, Long hackathonId) {
        return teamMemberRepository
                .findTeamByUserIdAndHackathonId(user.getId(), hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You must be an accepted team member to submit."));
    }

    /**
     * Saves the uploaded file to uploads/submissions/{teamId}/.
     * Returns the relative path stored in DB, or null if no file was given.
     */
    private String saveFile(MultipartFile file, Long teamId) {
        if (file == null || file.isEmpty()) return null;
        try {
            Path dir = Paths.get(System.getProperty("user.dir"),
                                  "uploads", "submissions", teamId.toString());
            Files.createDirectories(dir);

            String original  = file.getOriginalFilename();
            String extension = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf(".")) : "";
            String unique = UUID.randomUUID() + extension;

            Files.copy(file.getInputStream(), dir.resolve(unique));

            String rel = "uploads/submissions/" + teamId + "/" + unique;
            log.info("File saved: {}", rel);
            return rel;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save file: " + e.getMessage());
        }
    }

    private SubmissionResponse toResponse(Submission s) {
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
}