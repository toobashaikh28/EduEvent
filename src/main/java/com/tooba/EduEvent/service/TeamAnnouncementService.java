package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.ContentRequest;
import com.tooba.EduEvent.dto.response.AnnouncementResponse;
import java.util.List;

public interface TeamAnnouncementService {
    void createAnnouncement(String teamId, String email, ContentRequest request);
    List<AnnouncementResponse> getTeamAnnouncements(String teamId, String email);
    void addComment(String announcementId, String email, ContentRequest request);
}