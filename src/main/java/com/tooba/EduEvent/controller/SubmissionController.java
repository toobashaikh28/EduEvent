package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.request.SubmissionRequest;
import com.tooba.EduEvent.dto.response.SubmissionResponse;
import com.tooba.EduEvent.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/hackathon/{id}")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    // POST /api/hackathon/{id}/submit
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> submit(
            @PathVariable("id") Long hackathonId,
            @Valid @ModelAttribute SubmissionRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(submissionService.submit(hackathonId, authentication.getName(), request, file));
    }

    // PUT /api/hackathon/{id}/submit
    @PutMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> editSubmission(
            @PathVariable("id") Long hackathonId,
            @Valid @ModelAttribute SubmissionRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication authentication) {

        return ResponseEntity.ok(
                submissionService.editSubmission(hackathonId, authentication.getName(), request, file));
    }

    // GET /api/hackathon/{id}/my-submission
    @GetMapping("/my-submission")
    public ResponseEntity<SubmissionResponse> getMySubmission(
            @PathVariable("id") Long hackathonId,
            Authentication authentication) {

        return ResponseEntity.ok(
                submissionService.getMySubmission(hackathonId, authentication.getName()));
    }
}