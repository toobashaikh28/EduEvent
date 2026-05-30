package com.tooba.EduEvent;

import com.tooba.EduEvent.dto.request.ScoreRequest;
import com.tooba.EduEvent.dto.response.ScoreResponse;
import com.tooba.EduEvent.dto.response.SubmissionResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.impl.JudgeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeServiceTest {

    @Mock private UserRepository            userRepository;
    @Mock private EventRepository           eventRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private SubmissionRepository      submissionRepository;
    @Mock private ScoreRepository           scoreRepository;

    @InjectMocks
    private JudgeServiceImpl judgeService;

    private User admin;
    private User judge;
    private User nonJudge;
    private Event hackathon;
    private Team team;
    private Submission submission;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).name("Admin").email("admin@test.com")
                .password("hashed").role("ADMIN").build();

        judge = User.builder().id(2L).name("Judge Jane").email("judge@test.com")
                .password("hashed").role("JUDGE").build();

        nonJudge = User.builder().id(3L).name("Regular User").email("user@test.com")
                .password("hashed").role("USER").build();

        hackathon = Event.builder().id(10L).title("Spring Hack").type("Hackathon")
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().plusHours(10))
                .status("LIVE").admin(admin).build();

        team = Team.builder().id(5L).name("Team Alpha").hackathon(hackathon)
                .leader(nonJudge).isLocked(true).maxSize(4).build();

        submission = Submission.builder().id(100L).team(team).hackathon(hackathon)
                .title("My Project").description("Cool app")
                .githubUrl("https://github.com/team/project")
                .submittedAt(LocalDateTime.now()).build();
    }

    // ── Test 1: assign judge → succeeds ──────────────────────────────────
    @Test
    @DisplayName("Admin assigns a JUDGE-role user → assignment saved")
    void assignJudge_succeeds() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(hackathon));
        when(userRepository.findById(2L)).thenReturn(Optional.of(judge));
        when(judgeAssignmentRepository.existsByJudgeIdAndHackathonId(2L, 10L)).thenReturn(false);
        when(judgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> judgeService.assignJudge(10L, 2L, "admin@test.com"));
        verify(judgeAssignmentRepository).save(any(JudgeAssignment.class));
    }

    // ── Test 2: non-admin tries to assign → 403 ──────────────────────────
    @Test
    @DisplayName("Non-admin tries to assign judge → 403 FORBIDDEN")
    void assignJudge_nonAdmin_throws403() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(nonJudge));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> judgeService.assignJudge(10L, 2L, "user@test.com"));
        assertEquals(403, ex.getStatusCode().value());
    }

    // ── Test 3: assign user without JUDGE role → 400 ─────────────────────
    @Test
    @DisplayName("Assigning a non-JUDGE user → 400 BAD_REQUEST")
    void assignJudge_userNotJudgeRole_throws400() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(hackathon));
        when(userRepository.findById(3L)).thenReturn(Optional.of(nonJudge));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> judgeService.assignJudge(10L, 3L, "admin@test.com"));
        assertEquals(400, ex.getStatusCode().value());
    }

    // ── Test 4: judge GETs submissions → returns all for hackathon ────────
    @Test
    @DisplayName("Assigned judge GETs submissions → full list returned")
    void getSubmissionsForJudge_returnsAll() {
        when(userRepository.findByEmail("judge@test.com")).thenReturn(Optional.of(judge));
        when(judgeAssignmentRepository.existsByJudgeIdAndHackathonId(2L, 10L)).thenReturn(true);
        when(submissionRepository.findAllByHackathonId(10L)).thenReturn(List.of(submission));

        List<SubmissionResponse> results = judgeService.getSubmissionsForJudge(10L, "judge@test.com");

        assertEquals(1, results.size());
        assertEquals("My Project", results.get(0).getTitle());
        assertEquals("Team Alpha", results.get(0).getTeamName());
    }

    // ── Test 5: unassigned judge tries to view submissions → 403 ─────────
    @Test
    @DisplayName("Unassigned judge tries to view submissions → 403")
    void getSubmissions_notAssigned_throws403() {
        when(userRepository.findByEmail("judge@test.com")).thenReturn(Optional.of(judge));
        when(judgeAssignmentRepository.existsByJudgeIdAndHackathonId(2L, 10L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> judgeService.getSubmissionsForJudge(10L, "judge@test.com"));
        assertEquals(403, ex.getStatusCode().value());
    }

    // ── Test 6: judge POSTs a score → score saved ────────────────────────
    @Test
    @DisplayName("Judge POSTs score → Score entity saved via builder")
    void scoreSubmission_succeeds() {
        when(userRepository.findByEmail("judge@test.com")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(judgeAssignmentRepository.existsByJudgeIdAndHackathonId(2L, 10L)).thenReturn(true);
        when(scoreRepository.existsByJudgeIdAndSubmissionId(2L, 100L)).thenReturn(false);
        when(scoreRepository.save(any())).thenAnswer(inv -> {
            Score s = inv.getArgument(0);
            // Simulate DB assigning id
            return Score.builder()
                    .id(1L).judge(s.getJudge()).submission(s.getSubmission())
                    .scoreValue(s.getScoreValue()).feedback(s.getFeedback())
                    .scoredAt(LocalDateTime.now()).build();
        });

        ScoreRequest req = new ScoreRequest();
        req.setSubmissionId(100L);
        req.setScoreValue(85);
        req.setFeedback("Great work!");

        ScoreResponse result = judgeService.scoreSubmission(req, "judge@test.com");

        assertEquals(85, result.getScoreValue());
        assertEquals("Great work!", result.getFeedback());
        assertEquals("Team Alpha", result.getTeamName());
        assertEquals("Judge Jane", result.getJudgeName());
        verify(scoreRepository).save(any(Score.class));
    }

    // ── Test 7: duplicate score → 409 ────────────────────────────────────
    @Test
    @DisplayName("Judge scores same submission twice → 409 CONFLICT")
    void scoreSubmission_duplicate_throws409() {
        when(userRepository.findByEmail("judge@test.com")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(judgeAssignmentRepository.existsByJudgeIdAndHackathonId(2L, 10L)).thenReturn(true);
        when(scoreRepository.existsByJudgeIdAndSubmissionId(2L, 100L)).thenReturn(true);

        ScoreRequest req = new ScoreRequest();
        req.setSubmissionId(100L);
        req.setScoreValue(90);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> judgeService.scoreSubmission(req, "judge@test.com"));
        assertEquals(409, ex.getStatusCode().value());
    }

    // ── Test 8: getMyScores → verify scores saved and returned ──────────
    @Test
    @DisplayName("Judge GETs their own scores → list returned correctly")
    void getMyScores_returnsJudgeScores() {
        Score score = Score.builder().id(1L).judge(judge).submission(submission)
                .scoreValue(78).feedback("Good effort").scoredAt(LocalDateTime.now()).build();

        when(userRepository.findByEmail("judge@test.com")).thenReturn(Optional.of(judge));
        when(judgeAssignmentRepository.existsByJudgeIdAndHackathonId(2L, 10L)).thenReturn(true);
        when(scoreRepository.findAllByJudgeIdAndSubmissionHackathonId(2L, 10L))
                .thenReturn(List.of(score));

        List<ScoreResponse> results = judgeService.getMyScores(10L, "judge@test.com");

        assertEquals(1, results.size());
        assertEquals(78, results.get(0).getScoreValue());
        assertEquals("Judge Jane", results.get(0).getJudgeName());
    }
}