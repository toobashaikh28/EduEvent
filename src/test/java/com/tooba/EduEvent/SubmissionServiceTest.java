package com.tooba.EduEvent;

import com.tooba.EduEvent.dto.request.SubmissionRequest;
import com.tooba.EduEvent.dto.response.SubmissionResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.impl.SubmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private SubmissionRepository  submissionRepository;
    @Mock private EventRepository       eventRepository;
    @Mock private UserRepository        userRepository;
    @Mock private TeamMemberRepository  teamMemberRepository;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private User user;
    private Event hackathon;
    private Team lockedTeam;
    private Team unlockedTeam;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Alice").email("alice@test.com")
                .password("hashed").role("USER").build();

        User leader = User.builder().id(2L).name("Leader").email("leader@test.com")
                .password("hashed").role("USER").build();

        hackathon = Event.builder().id(10L).title("Spring Hack").type("Hackathon")
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().plusHours(10))
                .status("LIVE").admin(leader).build();

        lockedTeam = Team.builder().id(5L).name("Team Alpha").hackathon(hackathon)
                .leader(leader).isLocked(true).maxSize(4).build();

        unlockedTeam = Team.builder().id(6L).name("Team Beta").hackathon(hackathon)
                .leader(leader).isLocked(false).maxSize(4).build();
    }

    private SubmissionRequest req() {
        SubmissionRequest r = new SubmissionRequest();
        r.setTitle("My Project");
        r.setDescription("Awesome app");
        r.setGithubUrl("https://github.com/alice/my-project");
        return r;
    }

    private Submission saved(Team team) {
        return Submission.builder().id(100L).team(team).hackathon(hackathon)
                .title("My Project").description("Awesome app")
                .githubUrl("https://github.com/alice/my-project")
                .filePath(null).submittedAt(LocalDateTime.now()).build();
    }

    // ── Test 1: submit with GitHub URL + no file → saved correctly ─────────
    @Test
    @DisplayName("Submit with GitHub URL, no file → saved and returned")
    void submit_githubUrl_noFile_succeeds() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(hackathon));
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(teamMemberRepository.findTeamByUserIdAndHackathonId(1L, 10L))
                .thenReturn(Optional.of(lockedTeam));
        when(submissionRepository.findByTeamIdAndHackathonId(5L, 10L))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any())).thenReturn(saved(lockedTeam));

        SubmissionResponse result = submissionService.submit(10L, "alice@test.com", req(), null);

        assertNotNull(result);
        assertEquals("My Project", result.getTitle());
        assertEquals("https://github.com/alice/my-project", result.getGithubUrl());
        assertEquals("Team Alpha", result.getTeamName());
        assertNull(result.getFilePath());
        verify(submissionRepository).save(any(Submission.class));
    }

    // ── Test 2: edit submission before deadline → fields updated ──────────
    @Test
    @DisplayName("Edit submission before deadline → updated correctly")
    void editSubmission_beforeDeadline_updatesFields() {
        Submission existing = saved(lockedTeam);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(hackathon));
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(teamMemberRepository.findTeamByUserIdAndHackathonId(1L, 10L))
                .thenReturn(Optional.of(lockedTeam));
        when(submissionRepository.findByTeamIdAndHackathonId(5L, 10L))
                .thenReturn(Optional.of(existing));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionRequest edit = new SubmissionRequest();
        edit.setTitle("Updated Title");
        edit.setDescription("New description");
        edit.setGithubUrl("https://github.com/alice/updated");

        SubmissionResponse result = submissionService.editSubmission(10L, "alice@test.com", edit, null);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("https://github.com/alice/updated", result.getGithubUrl());
        verify(submissionRepository).save(any(Submission.class));
    }

    // ── Test 3: team not locked → 400 ────────────────────────────────────
    @Test
    @DisplayName("Submit blocked when team is not locked → 400")
    void submit_teamNotLocked_throws400() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(hackathon));
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(teamMemberRepository.findTeamByUserIdAndHackathonId(1L, 10L))
                .thenReturn(Optional.of(unlockedTeam));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> submissionService.submit(10L, "alice@test.com", req(), null));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("locked"));
    }

    // ── Test 4: deadline passed → 400 ────────────────────────────────────
    @Test
    @DisplayName("Submit blocked after deadline → 400")
    void submit_afterDeadline_throws400() {
        Event expired = Event.builder().id(10L).title("Old Hack").type("Hackathon")
                .startTime(LocalDateTime.now().minusDays(3))
                .endTime(LocalDateTime.now().minusHours(1))
                .status("COMPLETED").admin(user).build();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(expired));
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(teamMemberRepository.findTeamByUserIdAndHackathonId(1L, 10L))
                .thenReturn(Optional.of(lockedTeam));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> submissionService.submit(10L, "alice@test.com", req(), null));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("deadline"));
    }

    // ── Test 5: duplicate submission → 409 ───────────────────────────────
    @Test
    @DisplayName("Second submit by same team → 409 CONFLICT")
    void submit_duplicate_throws409() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(hackathon));
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(teamMemberRepository.findTeamByUserIdAndHackathonId(1L, 10L))
                .thenReturn(Optional.of(lockedTeam));
        when(submissionRepository.findByTeamIdAndHackathonId(5L, 10L))
                .thenReturn(Optional.of(saved(lockedTeam)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> submissionService.submit(10L, "alice@test.com", req(), null));
        assertEquals(409, ex.getStatusCode().value());
    }

    // ── Test 6: user not in team → 403 ───────────────────────────────────
    @Test
    @DisplayName("User with no team → 403 FORBIDDEN")
    void submit_noTeam_throws403() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(hackathon));
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(teamMemberRepository.findTeamByUserIdAndHackathonId(1L, 10L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> submissionService.submit(10L, "alice@test.com", req(), null));
        assertEquals(403, ex.getStatusCode().value());
    }

    // ── Test 7: getMySubmission returns correct data ──────────────────────
    @Test
    @DisplayName("getMySubmission returns team's submission")
    void getMySubmission_returnsData() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(teamMemberRepository.findTeamByUserIdAndHackathonId(1L, 10L))
                .thenReturn(Optional.of(lockedTeam));
        when(submissionRepository.findByTeamIdAndHackathonId(5L, 10L))
                .thenReturn(Optional.of(saved(lockedTeam)));

        SubmissionResponse result = submissionService.getMySubmission(10L, "alice@test.com");
        assertEquals(100L, result.getId());
        assertEquals("Team Alpha", result.getTeamName());
    }

    // ── Test 8: edit after deadline → 400 ────────────────────────────────
    @Test
    @DisplayName("Edit after deadline → 400")
    void editSubmission_afterDeadline_throws400() {
        Event expired = Event.builder().id(10L).title("Old Hack").type("Hackathon")
                .startTime(LocalDateTime.now().minusDays(3))
                .endTime(LocalDateTime.now().minusHours(1))
                .status("COMPLETED").admin(user).build();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(expired));
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(teamMemberRepository.findTeamByUserIdAndHackathonId(1L, 10L))
                .thenReturn(Optional.of(lockedTeam));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> submissionService.editSubmission(10L, "alice@test.com", req(), null));
        assertEquals(400, ex.getStatusCode().value());
    }
}