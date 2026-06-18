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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamAnnouncementServiceImpl implements TeamAnnouncementService {

    private final TeamAnnouncementRepository announcementRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;

    @Override
    public void createAnnouncement(String teamId, String email, ContentRequest request) {
        User user = getUser(email);
        Team team = getTeam(teamId);

        if (!team.getLeaderId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the team leader can post announcements");
        }

        TeamAnnouncement announcement = TeamAnnouncement.builder()
                .teamId(team.getId())
                .authorId(user.getId())
                .content(request.getContent())
                .build();

        announcementRepository.save(announcement);

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdAndStatus(teamId, "ACCEPTED");
        for (TeamMember member : members) {
            if (!member.getUserId().equals(user.getId())) {
                notificationService.send(
                        member.getUserId(),
                        "New Announcement in " + team.getName(),
                        user.getName() + " posted a new team announcement."
                );
            }
        }
        log.info("Leader {} posted an announcement for team {}", user.getName(), teamId);
    }

    @Override
    public List<AnnouncementResponse> getTeamAnnouncements(String teamId, String email) {
        User user = getUser(email);
        verifyUserInTeam(teamId, user.getId());

        List<TeamAnnouncement> announcements = announcementRepository.findAllByTeamIdOrderByCreatedAtDesc(teamId);

        return announcements.stream().map(a -> AnnouncementResponse.builder()
                .id(a.getId())
                .authorName(userName(a.getAuthorId()))
                .content(a.getContent())
                .createdAt(a.getCreatedAt())
                .comments(a.getComments().stream().map(c -> AnnouncementResponse.CommentResponse.builder()
                        .id(c.getId())
                        .authorName(userName(c.getAuthorId()))
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .build()).collect(Collectors.toList()))
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public void addComment(String announcementId, String email, ContentRequest request) {
        User user = getUser(email);
        TeamAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        verifyUserInTeam(announcement.getTeamId(), user.getId());

        AnnouncementComment comment = AnnouncementComment.builder()
                .id(UUID.randomUUID().toString())
                .authorId(user.getId())
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        announcement.getComments().add(comment);
        announcementRepository.save(announcement);
        log.info("User {} added a comment to announcement {}", user.getName(), announcementId);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Team getTeam(String teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }

    private String userName(String userId) {
        return userRepository.findById(userId).map(User::getName).orElse("Unknown");
    }

    private void verifyUserInTeam(String teamId, String userId) {
        Team team = getTeam(teamId);
        boolean isLeader = team.getLeaderId().equals(userId);
        boolean isMember = teamMemberRepository.findAllByTeamIdAndStatus(teamId, "ACCEPTED")
                .stream().anyMatch(m -> m.getUserId().equals(userId));

        if (!isLeader && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this team");
        }
    }
}
