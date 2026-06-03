package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.request.ContentRequest;
import com.tooba.EduEvent.dto.response.AnnouncementResponse;
import com.tooba.EduEvent.service.TeamAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamAnnouncementController {

    private final TeamAnnouncementService announcementService;

    @PostMapping("/{id}/announcement")
    public ResponseEntity<String> createAnnouncement(
            @PathVariable Long id,
            @RequestBody ContentRequest request,
            Authentication authentication) {
        
        announcementService.createAnnouncement(id, authentication.getName(), request);
        return ResponseEntity.ok("Announcement posted successfully");
    }

    @GetMapping("/{id}/announcements")
    public ResponseEntity<List<AnnouncementResponse>> getAnnouncements(
            @PathVariable Long id,
            Authentication authentication) {
        
        return ResponseEntity.ok(announcementService.getTeamAnnouncements(id, authentication.getName()));
    }

    @PostMapping("/announcement/{announcementId}/comment")
    public ResponseEntity<String> addComment(
            @PathVariable Long announcementId,
            @RequestBody ContentRequest request,
            Authentication authentication) {
        
        announcementService.addComment(announcementId, authentication.getName(), request);
        return ResponseEntity.ok("Comment added successfully");
    }
}