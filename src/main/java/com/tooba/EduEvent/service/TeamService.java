package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.TeamCreateRequest;
import com.tooba.EduEvent.dto.request.TeamJoinRequest;
import com.tooba.EduEvent.dto.response.TeamResponse;
import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.Team;
import com.tooba.EduEvent.entity.TeamMember;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.EventRepository;
import com.tooba.EduEvent.repository.TeamMemberRepository;
import com.tooba.EduEvent.repository.TeamRepository;
import com.tooba.EduEvent.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {

    @Autowired private TeamRepository teamRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private NotificationService notificationService;

    private final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final Random random = new Random();

    private String generateSixCharJoinCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(ALPHA_NUMERIC.charAt(random.nextInt(ALPHA_NUMERIC.length())));
        }
        return code.toString();
    }

    @Transactional
    public TeamResponse createTeam(Long hackathonId, TeamCreateRequest request, Long leaderId) {
        Event hackathon = eventRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon event not found"));
        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (teamMemberRepository.existsByUserIdAndHackathonId(leaderId, hackathonId)) {
            throw new IllegalStateException("You are already registered in a team context for this hackathon.");
        }

        Team team = Team.builder()
                .name(request.getTeamName())
                .joinCode(generateSixCharJoinCode())
                .hackathon(hackathon)
                .leader(leader)
                .isLocked(false)
                .maxSize(4)
                .build();

        Team savedTeam = teamRepository.save(team);

        TeamMember leaderRoster = TeamMember.builder()
                .team(savedTeam)
                .user(leader)
                .role("LEADER")
                .status("ACCEPTED")
                .build();
        teamMemberRepository.save(leaderRoster);

        return mapToTeamResponse(savedTeam);
    }

    @Transactional
    public void acceptJoinRequest(Long memberId, Long leaderId) {
        TeamMember memberSlot = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Team member record not found"));

        Team team = memberSlot.getTeam();

        // Validation Check: Only the designated team leader can approve pending candidates
        if (!team.getLeader().getId().equals(leaderId)) {
            throw new RuntimeException("Access denied. Only the team leader can accept members.");
        }

        if (!"PENDING".equals(memberSlot.getStatus())) {
            throw new IllegalStateException("This join request has already been processed.");
        }

        // Validate capacity constraints before accepting
        long currentAcceptedCount = teamMemberRepository.countByTeamIdAndStatus(team.getId(), "ACCEPTED");
        if (currentAcceptedCount >= team.getMaxSize()) {
            throw new IllegalStateException("Cannot accept member. The team is already full.");
        }

        // 1. Update member status to ACCEPTED
        memberSlot.setStatus("ACCEPTED");
        teamMemberRepository.save(memberSlot);
        
        // Increment count to reflect the newly added member
        currentAcceptedCount++; 

        // 2. Auto-Lock Constraint Requirement: check if team memberCount == max_size -> auto set isLocked = true
        if (currentAcceptedCount >= team.getMaxSize()) {
            team.setIsLocked(true);
            teamRepository.save(team);
        }

        // 3. Send notification to user when their join request is accepted
        String notificationTitle = "Team Request Approved!";
        String alertMessage = String.format("Congratulations! Your request to join team '%s' has been accepted.", team.getName());
        notificationService.send(memberSlot.getUser().getId(), notificationTitle, alertMessage);
    }

    @Transactional
    public void rejectJoinRequest(Long memberId, Long leaderId) {
        TeamMember memberSlot = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Team member record not found"));

        Team team = memberSlot.getTeam();

        // Validation Check: Only the designated team leader can reject candidates
        if (!team.getLeader().getId().equals(leaderId)) {
            throw new RuntimeException("Access denied. Only the team leader can reject members.");
        }

        if (!"PENDING".equals(memberSlot.getStatus())) {
            throw new IllegalStateException("This join request has already been processed.");
        }

        // 1. Update member status to REJECTED
        memberSlot.setStatus("REJECTED");
        teamMemberRepository.save(memberSlot);

        // 2. Send notification to user when their join request is rejected
        String notificationTitle = "Team Request Declined";
        String alertMessage = String.format("Your request to join team '%s' was declined by the team leader.", team.getName());
        notificationService.send(memberSlot.getUser().getId(), notificationTitle, alertMessage);
    }


    @Transactional(readOnly = true)
    public TeamResponse getMyTeam(Long hackathonId, Long userId) {
        Team team = teamMemberRepository.findTeamByUserIdAndHackathonId(userId, hackathonId)
                .orElseThrow(() -> new RuntimeException("You are not currently part of a team for this hackathon."));
        
        return mapToTeamResponse(team);
    }
    

    @Transactional
    public void leaveTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Validation Check: If team is locked, users cannot leave
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new IllegalStateException("Cannot leave the team. The team configuration has been locked.");
        }

        // Prevent the leader from simply leaving without assigning someone else or dissolving the team
        if (team.getLeader().getId().equals(userId)) {
            throw new IllegalStateException("As the Team Leader, you cannot leave the team. You must dissolve it or transfer leadership instead.");
        }

        TeamMember memberRecord = teamMemberRepository.findAllByTeamIdAndStatus(team.getId(), "ACCEPTED")
                .stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("You are not an active member of this team."));

        teamMemberRepository.delete(memberRecord);

        // Notify the team leader that someone left
        String title = "Member Left Team";
        String message = String.format("Notification: %s has left your team '%s'.", memberRecord.getUser().getName(), team.getName());
        notificationService.send(team.getLeader().getId(), title, message);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeamsForHackathon(Long hackathonId, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Administrative Authorization Check
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Access denied. Only administrators can view all hackathon teams.");
        }

        return teamRepository.findAllByHackathonId(hackathonId)
                .stream()
                .map(this::mapToTeamResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void joinTeamRequest(Long hackathonId, TeamJoinRequest request, Long userId) {
        Team team = teamRepository.findByJoinCode(request.getInviteCode())
                .orElseThrow(() -> new RuntimeException("The join code provided is invalid"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile registry missing"));

        if (!team.getHackathon().getId().equals(hackathonId)) {
            throw new IllegalArgumentException("This code belongs to a team in a different event.");
        }
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new IllegalStateException("This team configuration has been locked by the leader.");
        }
        if (teamMemberRepository.existsByUserIdAndHackathonId(userId, hackathonId)) {
            throw new IllegalStateException("You are already associated with a team roster in this hackathon.");
        }

        long activeCount = teamMemberRepository.countByTeamIdAndStatus(team.getId(), "ACCEPTED");
        if (activeCount >= team.getMaxSize()) {
            throw new IllegalStateException("This team has already reached its maximum capacity limit.");
        }

        TeamMember pendingMember = TeamMember.builder()
                .team(team)
                .user(user)
                .role("MEMBER")
                .status("PENDING")
                .build();
        teamMemberRepository.save(pendingMember);

        String title = "New Team Join Request";
        // FIXED: Replaced .getFullName() with .getName()
        String alertText = String.format("%s has requested to join your team '%s'.", 
                user.getName(), team.getName());
        
        notificationService.send(team.getLeader().getId(), title, alertText);
    }

    private TeamResponse mapToTeamResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setTeamId(team.getId());
        response.setTeamName(team.getName());
        response.setInviteCode(team.getJoinCode());
        // FIXED: Replaced .getFullName() with .getName()
        response.setLeaderName(team.getLeader().getName());
        
        // FIXED: Replaced .getFullName() with .getName() inside the stream mapping
        response.setMemberNames(
            teamMemberRepository.findAllByTeamIdAndStatus(team.getId(), "ACCEPTED")
                .stream()
                .map(m -> m.getUser().getName())
                .collect(Collectors.toList())
        );
        return response;
    }
}