package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.ContentRequest;
import com.tooba.EduEvent.dto.response.AnnouncementResponse;
import java.util.List;

public interface TeamAnnouncementService {
    void createAnnouncement(Long teamId, String email, ContentRequest request);
    List<AnnouncementResponse> getTeamAnnouncements(Long teamId, String email);
    void addComment(Long announcementId, String email, ContentRequest request);
}