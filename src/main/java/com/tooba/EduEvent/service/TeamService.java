package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.TeamCreateRequest;
import com.tooba.EduEvent.dto.request.TeamJoinRequest;
import com.tooba.EduEvent.dto.response.MemberDto;
import com.tooba.EduEvent.dto.response.TeamResponse;
import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.RegistrationStatus;
import com.tooba.EduEvent.entity.Team;
import com.tooba.EduEvent.entity.TeamMember;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.EventRepository;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.repository.TeamMemberRepository;
import com.tooba.EduEvent.repository.TeamRepository;
import com.tooba.EduEvent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final RegistrationRepository registrationRepository;   // NEW — enforce "register first"

    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final Random random = new Random();

    private String generateSixCharJoinCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(ALPHA_NUMERIC.charAt(random.nextInt(ALPHA_NUMERIC.length())));
        }
        return code.toString();
    }

    private User resolveUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private String userName(String userId) {
        return userRepository.findById(userId).map(User::getName).orElse("Unknown");
    }

    // FIX: only ACCEPTED/PENDING membership blocks a user. A REJECTED user can
    // request again — previously findByUserId matched rejected rows too, so a
    // user rejected once could NEVER join any team in that hackathon.
    private boolean isUserInHackathon(String userId, String hackathonId) {
        return teamMemberRepository.findByUserId(userId).stream()
                .filter(m -> "ACCEPTED".equals(m.getStatus()) || "PENDING".equals(m.getStatus()))
                .anyMatch(m -> {
                    Team t = teamRepository.findById(m.getTeamId()).orElse(null);
                    return t != null && hackathonId.equals(t.getHackathonId());
                });
    }

    // FIX (feature list): "User registers for the hackathon event first"
    private void requireHackathonRegistration(String userId, String hackathonId) {
        boolean registered = registrationRepository
                .existsByUserIdAndEventIdAndStatus(userId, hackathonId, RegistrationStatus.REGISTERED);
        if (!registered) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You must register for this hackathon event before creating or joining a team.");
        }
    }

    // The user's ACCEPTED team within a hackathon
    private Optional<Team> findUserTeamInHackathon(String userId, String hackathonId) {
        return teamMemberRepository.findByUserIdAndStatus(userId, "ACCEPTED").stream()
                .map(m -> teamRepository.findById(m.getTeamId()).orElse(null))
                .filter(t -> t != null && hackathonId.equals(t.getHackathonId()))
                .findFirst();
    }

    public TeamResponse createTeam(String hackathonId, TeamCreateRequest request, String leaderEmail) {
        Event hackathon = eventRepository.findById(hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hackathon event not found"));
        User leader = resolveUserByEmail(leaderEmail);

        requireHackathonRegistration(leader.getId(), hackathonId);   // NEW

        if (isUserInHackathon(leader.getId(), hackathonId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You are already registered in a team context for this hackathon.");
        }

        // FIX (feature list): "Enters team name AND max size" — was hardcoded to 4.
        int maxSize = request.getMaxSize() != null ? request.getMaxSize() : 4;
        if (maxSize < 2 || maxSize > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Team max size must be between 2 and 10.");
        }

        Team team = Team.builder()
                .name(request.getTeamName())
                .joinCode(generateSixCharJoinCode())
                .hackathonId(hackathon.getId())
                .leaderId(leader.getId())
                .isLocked(false)
                .maxSize(maxSize)
                .build();

        Team savedTeam = teamRepository.save(team);

        TeamMember leaderRoster = TeamMember.builder()
                .teamId(savedTeam.getId())
                .userId(leader.getId())
                .role("LEADER")
                .status("ACCEPTED")
                .build();
        teamMemberRepository.save(leaderRoster);

        return mapToTeamResponse(savedTeam);
    }

    public void acceptJoinRequest(String memberId, String leaderEmail) {
        User leader = resolveUserByEmail(leaderEmail);
        TeamMember memberSlot = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team member record not found"));

        Team team = teamRepository.findById(memberSlot.getTeamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!team.getLeaderId().equals(leader.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the team leader can accept members.");
        }
        if (!"PENDING".equals(memberSlot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This join request has already been processed.");
        }

        long currentAcceptedCount = teamMemberRepository.countByTeamIdAndStatus(team.getId(), "ACCEPTED");
        if (currentAcceptedCount >= team.getMaxSize()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot accept member. The team is already full.");
        }

        memberSlot.setStatus("ACCEPTED");
        teamMemberRepository.save(memberSlot);
        currentAcceptedCount++;

        if (currentAcceptedCount >= team.getMaxSize()) {
            team.setIsLocked(true);
            teamRepository.save(team);
        }

        notificationService.send(memberSlot.getUserId(),
                "Team Request Approved!",
                "Congratulations! Your request to join team '" + team.getName() + "' has been accepted.");
    }

    public void rejectJoinRequest(String memberId, String leaderEmail) {
        User leader = resolveUserByEmail(leaderEmail);
        TeamMember memberSlot = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team member record not found"));

        Team team = teamRepository.findById(memberSlot.getTeamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));

        if (!team.getLeaderId().equals(leader.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the team leader can reject members.");
        }
        if (!"PENDING".equals(memberSlot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This join request has already been processed.");
        }

        memberSlot.setStatus("REJECTED");
        teamMemberRepository.save(memberSlot);

        notificationService.send(memberSlot.getUserId(),
                "Team Request Declined",
                "Your request to join team '" + team.getName() + "' was declined by the team leader.");
    }

    public TeamResponse getMyTeam(String hackathonId, String userEmail) {
        User user = resolveUserByEmail(userEmail);
        Team team = findUserTeamInHackathon(user.getId(), hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You are not currently part of a team for this hackathon."));
        return mapToTeamResponse(team);
    }

    public void leaveTeam(String teamId, String userEmail) {
        User user = resolveUserByEmail(userEmail);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
        doLeave(team, user);
    }

    // NEW — supports the frontend's POST /api/hackathon/{id}/team/leave call,
    // which only knows the hackathon id, not the team id.
    public void leaveTeamByHackathon(String hackathonId, String userEmail) {
        User user = resolveUserByEmail(userEmail);
        Team team = findUserTeamInHackathon(user.getId(), hackathonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You are not currently part of a team for this hackathon."));
        doLeave(team, user);
    }

    private void doLeave(Team team, User user) {
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot leave the team. The team configuration has been locked.");
        }
        if (team.getLeaderId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "As the Team Leader, you cannot leave the team. Dissolve it or transfer leadership instead.");
        }

        TeamMember memberRecord = teamMemberRepository.findAllByTeamIdAndStatus(team.getId(), "ACCEPTED")
                .stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You are not an active member of this team."));

        teamMemberRepository.delete(memberRecord);

        notificationService.send(team.getLeaderId(),
                "Member Left Team",
                user.getName() + " has left your team '" + team.getName() + "'.");
    }

    public List<TeamResponse> getAllTeamsForHackathon(String hackathonId, String adminEmail) {
        User admin = resolveUserByEmail(adminEmail);
        if (!"ADMIN".equals(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only administrators can view all hackathon teams.");
        }
        return teamRepository.findAllByHackathonId(hackathonId).stream()
                .map(this::mapToTeamResponse)
                .collect(Collectors.toList());
    }

    public void joinTeamRequest(String hackathonId, TeamJoinRequest request, String userEmail) {
        User user = resolveUserByEmail(userEmail);

        requireHackathonRegistration(user.getId(), hackathonId);   // NEW

        Team team = teamRepository.findByJoinCode(request.getInviteCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The join code provided is invalid"));

        if (!team.getHackathonId().equals(hackathonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This code belongs to a team in a different event.");
        }
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This team configuration has been locked by the leader.");
        }
        if (isUserInHackathon(user.getId(), hackathonId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You are already associated with a team roster in this hackathon.");
        }

        long activeCount = teamMemberRepository.countByTeamIdAndStatus(team.getId(), "ACCEPTED");
        if (activeCount >= team.getMaxSize()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This team has already reached its maximum capacity limit.");
        }

        TeamMember pendingMember = TeamMember.builder()
                .teamId(team.getId())
                .userId(user.getId())
                .role("MEMBER")
                .status("PENDING")
                .build();
        teamMemberRepository.save(pendingMember);

        notificationService.send(team.getLeaderId(),
                "New Team Join Request",
                user.getName() + " has requested to join your team '" + team.getName() + "'.");
    }

    /**
     * FIX (feature list): "All members listed with status badges (Leader/Member/Pending)".
     * Previously only ACCEPTED members were returned, so the leader could never SEE
     * pending join requests — making the whole join-a-team flow unusable from the UI.
     * Also now includes memberId (needed by the accept/reject endpoints), leaderId,
     * isLocked and maxSize (needed to render the lock state and capacity).
     */
    private TeamResponse mapToTeamResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setTeamId(team.getId());
        response.setTeamName(team.getName());
        response.setInviteCode(team.getJoinCode());
        response.setLeaderId(team.getLeaderId());
        response.setLeaderName(userName(team.getLeaderId()));
        response.setHackathonId(team.getHackathonId());
        response.setLocked(Boolean.TRUE.equals(team.getIsLocked()));
        response.setMaxSize(team.getMaxSize());

        List<MemberDto> members = new ArrayList<>();
        // Accepted first…
        teamMemberRepository.findAllByTeamIdAndStatus(team.getId(), "ACCEPTED")
                .forEach(m -> members.add(new MemberDto(
                        m.getId(), m.getUserId(), userName(m.getUserId()), m.getRole(), m.getStatus())));
        // …then pending join requests (visible to everyone; actionable by the leader)
        teamMemberRepository.findAllByTeamIdAndStatus(team.getId(), "PENDING")
                .forEach(m -> members.add(new MemberDto(
                        m.getId(), m.getUserId(), userName(m.getUserId()), m.getRole(), m.getStatus())));

        response.setMembers(members);
        return response;
    }
}
