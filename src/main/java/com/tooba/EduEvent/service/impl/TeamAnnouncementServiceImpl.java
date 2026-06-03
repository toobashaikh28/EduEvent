package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.request.ContentRequest;
import com.tooba.EduEvent.dto.response.AnnouncementResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.NotificationService;
import com.tooba.EduEvent.service.TeamAnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamAnnouncementServiceImpl implements TeamAnnouncementService {

    private final TeamAnnouncementRepository announcementRepository;
    private final AnnouncementCommentRepository commentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void createAnnouncement(Long teamId, String email, ContentRequest request) {
        User user = getUser(email);
        Team team = getTeam(teamId);

        if (!team.getLeader().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the team leader can post announcements");
        }

        TeamAnnouncement announcement = TeamAnnouncement.builder()
                .team(team)
                .author(user)
                .content(request.getContent())
                .build();
        
        announcementRepository.save(announcement);

        // Notify team members
        List<TeamMember> members = teamMemberRepository.findAllByTeamIdAndStatus(teamId, "ACCEPTED");
        for (TeamMember member : members) {
            if (!member.getUser().getId().equals(user.getId())) {
                notificationService.send(
                        member.getUser().getId(),
                        "New Announcement in " + team.getName(),
                        user.getName() + " posted a new team announcement."
                );
            }
        }
        log.info("Leader {} posted an announcement for team {}", user.getName(), teamId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getTeamAnnouncements(Long teamId, String email) {
        User user = getUser(email);
        verifyUserInTeam(teamId, user.getId());

        List<TeamAnnouncement> announcements = announcementRepository.findAllByTeamIdOrderByCreatedAtDesc(teamId);

        return announcements.stream().map(a -> AnnouncementResponse.builder()
                .id(a.getId())
                .authorName(a.getAuthor().getName())
                .content(a.getContent())
                .createdAt(a.getCreatedAt())
                .comments(a.getComments().stream().map(c -> AnnouncementResponse.CommentResponse.builder()
                        .id(c.getId())
                        .authorName(c.getAuthor().getName())
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .build()).collect(Collectors.toList()))
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addComment(Long announcementId, String email, ContentRequest request) {
        User user = getUser(email);
        TeamAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        verifyUserInTeam(announcement.getTeam().getId(), user.getId());

        AnnouncementComment comment = AnnouncementComment.builder()
                .announcement(announcement)
                .author(user)
                .content(request.getContent())
                .build();
        
        commentRepository.save(comment);
        log.info("User {} added a comment to announcement {}", user.getName(), announcementId);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Team getTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }

    private void verifyUserInTeam(Long teamId, Long userId) {
        Team team = getTeam(teamId);
        boolean isLeader = team.getLeader().getId().equals(userId);
        boolean isMember = teamMemberRepository.findAllByTeamIdAndStatus(teamId, "ACCEPTED")
                .stream().anyMatch(m -> m.getUser().getId().equals(userId));

        if (!isLeader && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
        }
    }
}