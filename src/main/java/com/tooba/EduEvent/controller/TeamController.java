package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.request.TeamCreateRequest;
import com.tooba.EduEvent.dto.request.TeamJoinRequest;
import com.tooba.EduEvent.dto.response.TeamResponse;
import com.tooba.EduEvent.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    // POST /api/hackathon/{hackathonId}/team/create
    @PostMapping("/api/hackathon/{hackathonId}/team/create")
    public ResponseEntity<TeamResponse> createTeam(
            @PathVariable("hackathonId") Long hackathonId,
            @Valid @RequestBody TeamCreateRequest request,
            Authentication authentication) {

        TeamResponse response = teamService.createTeam(hackathonId, request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // POST /api/hackathon/{hackathonId}/team/join
    @PostMapping("/api/hackathon/{hackathonId}/team/join")
    public ResponseEntity<String> joinTeam(
            @PathVariable("hackathonId") Long hackathonId,
            @Valid @RequestBody TeamJoinRequest request,
            Authentication authentication) {

        teamService.joinTeamRequest(hackathonId, request, authentication.getName());
        return ResponseEntity.ok("Your request to join the team has been sent to the team leader.");
    }

    // PUT /api/team/invite/{memberId}/accept
    @PutMapping("/api/team/invite/{memberId}/accept")
    public ResponseEntity<String> acceptMember(
            @PathVariable("memberId") Long memberId,
            Authentication authentication) {

        teamService.acceptJoinRequest(memberId, authentication.getName());
        return ResponseEntity.ok("Member request accepted successfully.");
    }

    // PUT /api/team/invite/{memberId}/reject
    @PutMapping("/api/team/invite/{memberId}/reject")
    public ResponseEntity<String> rejectMember(
            @PathVariable("memberId") Long memberId,
            Authentication authentication) {

        teamService.rejectJoinRequest(memberId, authentication.getName());
        return ResponseEntity.ok("Member request rejected successfully.");
    }

    // GET /api/hackathon/{hackathonId}/my-team
    @GetMapping("/api/hackathon/{hackathonId}/my-team")
    public ResponseEntity<TeamResponse> getMyTeam(
            @PathVariable("hackathonId") Long hackathonId,
            Authentication authentication) {

        TeamResponse response = teamService.getMyTeam(hackathonId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // DELETE /api/team/{teamId}/leave
    @DeleteMapping("/api/team/{teamId}/leave")
    public ResponseEntity<String> leaveTeam(
            @PathVariable("teamId") Long teamId,
            Authentication authentication) {

        teamService.leaveTeam(teamId, authentication.getName());
        return ResponseEntity.ok("You have successfully left the team.");
    }

    // GET /api/hackathon/{hackathonId}/teams  (admin only)
    @GetMapping("/api/hackathon/{hackathonId}/teams")
    public ResponseEntity<List<TeamResponse>> getAllTeams(
            @PathVariable("hackathonId") Long hackathonId,
            Authentication authentication) {

        List<TeamResponse> response = teamService.getAllTeamsForHackathon(hackathonId, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
