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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;

    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final Random random = new Random();

    private String generateSixCharJoinCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(ALPHA_NUMERIC.charAt(random.nextInt(ALPHA_NUMERIC.length())));
        }
        return code.toString();
    }

    // FIX 1: Accept email (from JWT) instead of raw userId from request param
    private User resolveUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @Transactional
    public TeamResponse createTeam(Long hackathonId, TeamCreateRequest request, String leaderEmail) {
        Event hackathon = eventRepository.findById(hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hackathon event not found"));
        User leader = resolveUserByEmail(leaderEmail);

        if (teamMemberRepository.existsByUserIdAndHackathonId(leader.getId(), hackathonId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You are already registered in a team context for this hackathon.");
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
    public void acceptJoinRequest(Long memberId, String leaderEmail) {
        User leader = resolveUserByEmail(leaderEmail);
        TeamMember memberSlot = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team member record not found"));

        Team team = memberSlot.getTeam();

        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the team leader can accept members.");
        }

        if (!"PENDING".equals(memberSlot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This join request has already been processed.");
        }

        long currentAcceptedCount = teamMemberRepository.countByTeamIdAndStatus(team.getId(), "ACCEPTED");
        if (currentAcceptedCount >= team.getMaxSize()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot accept member. The team is already full.");
        }

        memberSlot.setStatus("ACCEPTED");
        teamMemberRepository.save(memberSlot);
        currentAcceptedCount++;

        if (currentAcceptedCount >= team.getMaxSize()) {
            team.setIsLocked(true);
            teamRepository.save(team);
        }

        notificationService.send(memberSlot.getUser().getId(),
                "Team Request Approved!",
                "Congratulations! Your request to join team '" + team.getName() + "' has been accepted.");
    }

    @Transactional
    public void rejectJoinRequest(Long memberId, String leaderEmail) {
        User leader = resolveUserByEmail(leaderEmail);
        TeamMember memberSlot = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team member record not found"));

        Team team = memberSlot.getTeam();

        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the team leader can reject members.");
        }

        if (!"PENDING".equals(memberSlot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This join request has already been processed.");
        }

        memberSlot.setStatus("REJECTED");
        teamMemberRepository.save(memberSlot);

        notificationService.send(memberSlot.getUser().getId(),
                "Team Request Declined",
                "Your request to join team '" + team.getName() + "' was declined by the team leader.");
    }

    @Transactional(readOnly = true)
    public TeamResponse getMyTeam(Long hackathonId, String userEmail) {
        User user = resolveUserByEmail(userEmail);
        Team team = teamMemberRepository.findTeamByUserIdAndHackathonId(user.getId(), hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You are not currently part of a team for this hackathon."));
        return mapToTeamResponse(team);
    }

    @Transactional
    public void leaveTeam(Long teamId, String userEmail) {
        User user = resolveUserByEmail(userEmail);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot leave the team. The team configuration has been locked.");
        }

        if (team.getLeader().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "As the Team Leader, you cannot leave the team. Dissolve it or transfer leadership instead.");
        }

        TeamMember memberRecord = teamMemberRepository.findAllByTeamIdAndStatus(team.getId(), "ACCEPTED")
                .stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You are not an active member of this team."));

        teamMemberRepository.delete(memberRecord);

        notificationService.send(team.getLeader().getId(),
                "Member Left Team",
                memberRecord.getUser().getName() + " has left your team '" + team.getName() + "'.");
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeamsForHackathon(Long hackathonId, String adminEmail) {
        User admin = resolveUserByEmail(adminEmail);

        // FIX 5: use ResponseStatusException (403) instead of RuntimeException (→ 500)
        if (!"ADMIN".equals(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only administrators can view all hackathon teams.");
        }

        return teamRepository.findAllByHackathonId(hackathonId)
                .stream()
                .map(this::mapToTeamResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void joinTeamRequest(Long hackathonId, TeamJoinRequest request, String userEmail) {
        User user = resolveUserByEmail(userEmail);
        Team team = teamRepository.findByJoinCode(request.getInviteCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The join code provided is invalid"));

        if (!team.getHackathon().getId().equals(hackathonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This code belongs to a team in a different event.");
        }
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This team configuration has been locked by the leader.");
        }
        if (teamMemberRepository.existsByUserIdAndHackathonId(user.getId(), hackathonId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You are already associated with a team roster in this hackathon.");
        }

        long activeCount = teamMemberRepository.countByTeamIdAndStatus(team.getId(), "ACCEPTED");
        if (activeCount >= team.getMaxSize()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This team has already reached its maximum capacity limit.");
        }

        TeamMember pendingMember = TeamMember.builder()
                .team(team)
                .user(user)
                .role("MEMBER")
                .status("PENDING")
                .build();
        teamMemberRepository.save(pendingMember);

        notificationService.send(team.getLeader().getId(),
                "New Team Join Request",
                user.getName() + " has requested to join your team '" + team.getName() + "'.");
    }

    private TeamResponse mapToTeamResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setTeamId(team.getId());
        response.setTeamName(team.getName());
        response.setInviteCode(team.getJoinCode());
        response.setLeaderName(team.getLeader().getName());
        
        // Map the database entities to the new MemberDto object
        response.setMembers(
            teamMemberRepository.findAllByTeamIdAndStatus(team.getId(), "ACCEPTED")
                .stream()
                .map(m -> new com.tooba.EduEvent.dto.response.MemberDto(
                        m.getUser().getId(),
                        m.getUser().getName(),
                        m.getRole(),
                        m.getStatus()
                ))
                .collect(Collectors.toList())
        );
        return response;
    }
}
