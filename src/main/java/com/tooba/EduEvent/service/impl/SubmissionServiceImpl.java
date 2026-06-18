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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository  submissionRepository;
    private final EventRepository       eventRepository;
    private final UserRepository        userRepository;
    private final TeamRepository        teamRepository;
    private final TeamMemberRepository  teamMemberRepository;

    @Override
    public SubmissionResponse submit(String hackathonId, String userEmail,
                                     SubmissionRequest request, MultipartFile file) {
        Event hackathon = resolveHackathon(hackathonId);
        User  user      = resolveUser(userEmail);
        Team  team      = resolveTeam(user, hackathonId);

        if (!Boolean.TRUE.equals(team.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your team must be locked before submitting.");
        }
        if (java.time.LocalDateTime.now().isAfter(hackathon.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Submission deadline has passed (ended: " + hackathon.getEndTime() + ").");
        }
        if (submissionRepository.findByTeamIdAndHackathonId(team.getId(), hackathonId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Your team already has a submission. Use PUT to edit it.");
        }

        String filePath = saveFile(file, team.getId());

        Submission submission = Submission.builder()
                .teamId(team.getId())
                .hackathonId(hackathon.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .githubUrl(request.getGithubUrl())
                .filePath(filePath)
                .build();

        Submission saved = submissionRepository.save(submission);
        log.info("Submission created: id={} team={} hackathon={}", saved.getId(), team.getId(), hackathonId);
        return toResponse(saved);
    }

    @Override
    public SubmissionResponse editSubmission(String hackathonId, String userEmail,
                                             SubmissionRequest request, MultipartFile file) {
        Event hackathon = resolveHackathon(hackathonId);
        User  user      = resolveUser(userEmail);
        Team  team      = resolveTeam(user, hackathonId);

        if (java.time.LocalDateTime.now().isAfter(hackathon.getEndTime())) {
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

        if (file != null && !file.isEmpty()) {
            submission.setFilePath(saveFile(file, team.getId()));
        }

        Submission saved = submissionRepository.save(submission);
        log.info("Submission updated: id={} team={} hackathon={}", saved.getId(), team.getId(), hackathonId);
        return toResponse(saved);
    }

    @Override
    public SubmissionResponse getMySubmission(String hackathonId, String userEmail) {
        User user = resolveUser(userEmail);
        Team team = resolveTeam(user, hackathonId);

        return submissionRepository
                .findByTeamIdAndHackathonId(team.getId(), hackathonId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Your team has not submitted yet."));
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Event resolveHackathon(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Hackathon not found: " + id));
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
    }

    // The user's ACCEPTED team in this hackathon
    private Team resolveTeam(User user, String hackathonId) {
        return teamMemberRepository.findByUserIdAndStatus(user.getId(), "ACCEPTED").stream()
                .map(m -> teamRepository.findById(m.getTeamId()).orElse(null))
                .filter(t -> t != null && hackathonId.equals(t.getHackathonId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You must be an accepted team member to submit."));
    }

    private String saveFile(MultipartFile file, String teamId) {
        if (file == null || file.isEmpty()) return null;
        try {
            Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "submissions", teamId);
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
        String teamName = teamRepository.findById(s.getTeamId()).map(Team::getName).orElse("Unknown Team");
        String hackTitle = eventRepository.findById(s.getHackathonId()).map(Event::getTitle).orElse("Unknown Event");
        return SubmissionResponse.builder()
                .id(s.getId())
                .teamId(s.getTeamId())
                .teamName(teamName)
                .hackathonId(s.getHackathonId())
                .hackathonTitle(hackTitle)
                .title(s.getTitle())
                .description(s.getDescription())
                .githubUrl(s.getGithubUrl())
                .filePath(s.getFilePath())
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}
