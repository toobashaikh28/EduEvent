package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.request.QuestionRequest;
import com.tooba.EduEvent.dto.request.QuizRequest;
import com.tooba.EduEvent.dto.request.SubmitQuizRequest;
import com.tooba.EduEvent.dto.request.ViolationRequest;
import com.tooba.EduEvent.dto.response.*;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.CertificateService;
import com.tooba.EduEvent.service.LeaderboardService;
import org.springframework.cache.annotation.CacheEvict;
import com.tooba.EduEvent.mediator.NotificationMediator;
import com.tooba.EduEvent.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

    /** Maximum violations before session is invalidated (inclusive threshold). */
    private static final int MAX_VIOLATIONS = 3;

    private final QuizRepository quizRepository;
    private final LeaderboardService leaderboardService;
    private final QuestionRepository questionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final ViolationRepository violationRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final CertificateService certificateService;
    private final NotificationMediator notificationMediator;

    // ── CREATE QUIZ ───────────────────────────────────────────────────────────

    @Override
    public QuizResponse createQuiz(QuizRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found with id: " + request.getEventId()));

        Quiz quiz = Quiz.builder()
                .eventId(event.getId())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 30)
                .passScore(request.getPassScore() != null ? request.getPassScore() : new java.math.BigDecimal("50.00"))
                .randomize(request.getRandomize() != null ? request.getRandomize() : true)
                .build();
        return mapToQuizResponse(quizRepository.save(quiz));
    }

    @Override
    public List<QuizResponse> getAllQuizzes() {
        return quizRepository.findAll().stream()
                .map(this::mapToQuizResponseWithStats)
                .collect(Collectors.toList());
    }

    @Override
    public QuizResponse updateQuiz(String id, QuizRequest request) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        if (request.getDurationMinutes() != null) quiz.setDurationMinutes(request.getDurationMinutes());
        if (request.getPassScore() != null)       quiz.setPassScore(request.getPassScore());
        if (request.getRandomize() != null)        quiz.setRandomize(request.getRandomize());
        return mapToQuizResponseWithStats(quizRepository.save(quiz));
    }

    @Override
    public void deleteQuiz(String id) {
        if (!quizRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        // remove the quiz and its questions
        questionRepository.findByQuizId(id).forEach(q -> questionRepository.deleteById(q.getId()));
        quizRepository.deleteById(id);
    }

    @Override
    public QuestionResponse addQuestionToQuiz(String quizId, QuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        List<Option> options = request.getOptions().stream().map(o ->
                Option.builder()
                        .id(UUID.randomUUID().toString())
                        .optionText(o.getOptionText())
                        .isCorrect(o.isCorrect())
                        .build()
        ).collect(Collectors.toList());

        Question question = Question.builder()
                .quizId(quiz.getId())
                .questionText(request.getText())
                .options(options)
                .build();
        Question saved = questionRepository.save(question);

        return mapToQuestionResponse(saved);
    }

    @Override
    public List<QuestionResponse> getQuestionsByQuiz(String quizId) {
        quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        return questionRepository.findByQuizId(quizId).stream()
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteQuestion(String questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        questionRepository.deleteById(questionId);
    }

    // ── START QUIZ ────────────────────────────────────────────────────────────

    @Override
    public QuizSessionResponse startQuiz(String quizId, String userEmail) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Event event = eventRepository.findById(quiz.getEventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz event not found"));

        // 🛡️ Guard 1: the host event must be open (LIVE or ACTIVE)
        String evStatus = event.getStatus() != null ? event.getStatus().toUpperCase() : "";
        if (!evStatus.equals("LIVE") && !evStatus.equals("ACTIVE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This quiz isn't open yet — its event is not active.");
        }

        // (Open to all participants — no event-registration requirement.)

        // 🛡️ Guard: Block concurrent or already-passed sessions
        quizSessionRepository.findFirstByQuizIdAndUserIdAndStatusIn(quizId, user.getId(), List.of("ONGOING", "COMPLETED"))
                .ifPresent(existing -> {
                    if ("ONGOING".equals(existing.getStatus())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "You already have an ongoing session for this quiz.");
                    }
                    if ("COMPLETED".equals(existing.getStatus()) &&
                            existing.getScore() != null &&
                            existing.getScore() >= quiz.getPassScore().doubleValue()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "AlreadyPassed: You have already passed this quiz and achieved certification.");
                    }
                });

        QuizSession session = QuizSession.builder()
                .quizId(quiz.getId())
                .userId(user.getId())
                .startTime(LocalDateTime.now())
                .status("ONGOING")
                .build();
        QuizSession savedSession = quizSessionRepository.save(session);

        List<Question> questions = new ArrayList<>(questionRepository.findByQuizId(quizId));
        if (Boolean.TRUE.equals(quiz.getRandomize())) {
            Collections.shuffle(questions);
        }

        List<QuestionResponse> questionResponses = questions.stream().map(q -> {
            List<Option> opts = new ArrayList<>(q.getOptions());
            Collections.shuffle(opts);
            return QuestionResponse.builder()
                    .id(q.getId())
                    .text(q.getQuestionText())
                    .options(opts.stream().map(o -> OptionResponse.builder()
                            .id(o.getId())
                            .optionText(o.getOptionText())
                            .build()).collect(Collectors.toList()))
                    .build();
        }).collect(Collectors.toList());

        return QuizSessionResponse.builder()
                .sessionId(savedSession.getId())
                .durationMinutes(quiz.getDurationMinutes())
                .startTime(savedSession.getStartTime())
                .questions(questionResponses)
                .build();
    }

    // ── SUBMIT QUIZ ───────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "global-leaderboard", allEntries = true)
    public QuizResultResponse submitQuiz(String sessionId, SubmitQuizRequest request, String userEmail) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        User sessionUser = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session user not found"));

        // verify the session belongs to the caller
        if (!sessionUser.getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access Denied: You can only submit your own quiz session.");
        }

        if ("INVALIDATED".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "SessionInvalidated: Submission rejected. This trace has been terminated due to proctoring breaches.");
        }
        if (!"ONGOING".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This session is already " + session.getStatus());
        }

        Quiz quiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        long minutesElapsed = java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
        if (minutesElapsed > quiz.getDurationMinutes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "TimeLimitExceeded: Time limit exceeded. Your session has expired.");
        }

        Map<String, String> answers = request.getAnswers();
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        int totalCount = questions.size();
        int correctCount = 0;

        for (Question q : questions) {
            String selectedOptionId = answers != null ? answers.get(q.getId()) : null;
            if (selectedOptionId == null) continue;
            boolean isCorrect = q.getOptions().stream()
                    .anyMatch(o -> o.getId().equals(selectedOptionId) && Boolean.TRUE.equals(o.getIsCorrect()));
            if (isCorrect) correctCount++;
        }

        double score = totalCount > 0 ? ((double) correctCount / totalCount) * 100 : 0;
        boolean isPassed = score >= quiz.getPassScore().doubleValue();

        session.setScore(score);
        session.setEndTime(LocalDateTime.now());
        session.setStatus("COMPLETED");
        quizSessionRepository.save(session);

        // 🔥 Upsert into leaderboard
        leaderboardService.upsertQuizScore(sessionUser.getId(), quiz.getEventId(), score);

        // AUTO-TRIGGER CERTIFICATE IF PASSED
        if (isPassed) {
            try {
                Event event = eventRepository.findById(quiz.getEventId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
                certificateService.generate(sessionUser, event);
            } catch (Exception e) {
                log.error("Failed to generate certificate after passing quiz for session ID: {}", sessionId, e);
            }
        }

        // ── MEDIATOR: notify user of quiz result ─────────────────────────────
        notificationMediator.notify(
            this,
            isPassed ? "QUIZ_PASSED" : "QUIZ_FAILED",
            sessionUser.getId(),
            isPassed
                ? "You scored " + String.format("%.1f", score) + "% — you passed! 🎉"
                : "You scored " + String.format("%.1f", score) + "%. The pass mark was "
                    + quiz.getPassScore().doubleValue() + "%."
        );

        return QuizResultResponse.builder()
                .score(score)
                .passScore(quiz.getPassScore().doubleValue())
                .isPassed(isPassed)
                .correctCount(correctCount)
                .totalCount(totalCount)
                .build();
    }

    // ── HISTORICAL LOG LOOKUPS (User Profiles) ────────────────────────────────

    @Override
    public List<UserQuizResponse> getMyQuizzes(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Every quiz the admin has created is visible to every participant
        // This user's sessions grouped by quiz (for their personal attempt status)
        Map<String, List<QuizSession>> sessionsByQuiz = quizSessionRepository.findByUserId(user.getId())
                .stream().collect(Collectors.groupingBy(QuizSession::getQuizId));

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d");

        return quizRepository.findAll().stream().map(q -> {
            Event ev = eventRepository.findById(q.getEventId()).orElse(null);
            String title = ev != null ? ev.getTitle() : "Quiz";
            String evStatus = (ev != null && ev.getStatus() != null) ? ev.getStatus().toUpperCase() : "UPCOMING";
            String status = switch (evStatus) {
                case "LIVE", "ACTIVE" -> "active";
                case "UPCOMING"       -> "upcoming";
                default                -> "completed";
            };
            double pass = q.getPassScore() != null ? q.getPassScore().doubleValue() : 50.0;
            int qCount = questionRepository.findByQuizId(q.getId()).size();

            List<QuizSession> sess = sessionsByQuiz.getOrDefault(q.getId(), List.of());
            QuizSession best = sess.stream().filter(s -> s.getScore() != null)
                    .max(java.util.Comparator.comparingDouble(QuizSession::getScore)).orElse(null);
            boolean attempted = best != null || sess.stream()
                    .anyMatch(s -> "COMPLETED".equals(s.getStatus()) || "TIMED_OUT".equals(s.getStatus()));
            Integer score = best != null && best.getScore() != null ? (int) Math.round(best.getScore()) : null;
            boolean passed = score != null && score >= pass;
            String attemptDate = (best != null && best.getEndTime() != null) ? best.getEndTime().format(fmt) : null;

            return UserQuizResponse.builder()
                    .id(q.getId()).title(title).eventName(title)
                    .durationMinutes(q.getDurationMinutes()).questionCount(qCount)
                    .passMark((int) Math.round(pass)).status(status)
                    .attempted(attempted).score(score).passed(passed).attemptDate(attemptDate)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<QuizResultResponse> getMyQuizHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return quizSessionRepository.findByUserId(user.getId()).stream()
                .map(s -> {
                    double pass = quizRepository.findById(s.getQuizId())
                            .map(q -> q.getPassScore().doubleValue()).orElse(50.0);
                    return QuizResultResponse.builder()
                            .score(s.getScore())
                            .passScore(pass)
                            .isPassed(s.getScore() != null && s.getScore() >= pass)
                            .correctCount(0)
                            .totalCount(0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public QuizResultResponse getSessionResult(String sessionId, String userEmail) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        User sessionUser = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session user not found"));

        if (!sessionUser.getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Resource isolation breach.");
        }

        double pass = quizRepository.findById(session.getQuizId())
                .map(q -> q.getPassScore().doubleValue()).orElse(50.0);
        boolean isPassed = session.getScore() != null && session.getScore() >= pass;

        return QuizResultResponse.builder()
                .score(session.getScore())
                .passScore(pass)
                .isPassed(isPassed)
                .build();
    }

    // ── VIOLATION PROCTORING CONTROL ──────────────────────────────────────────

    @Override
    public ViolationResponse recordViolation(String sessionId, ViolationRequest request) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!"ONGOING".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot record violation for a session that is " + session.getStatus());
        }

        Violation violation = Violation.builder()
                .sessionId(session.getId())
                .type(request.getType())
                .build();
        Violation saved = violationRepository.save(violation);

        int totalCount = violationRepository.countBySessionId(sessionId);

        if (totalCount >= MAX_VIOLATIONS) {
            session.setStatus("INVALIDATED");
            quizSessionRepository.save(session);
        }

        return ViolationResponse.builder()
                .violationId(saved.getId())
                .totalCount(totalCount)
                .sessionStatus(session.getStatus())
                .build();
    }

    // ── ADMIN RESULTS OVERVIEW ────────────────────────────────────────────────

    @Override
    public List<AdminQuizResultResponse> getQuizResults(String quizId) {
        return quizSessionRepository.findByQuizId(quizId).stream()
                .map(s -> AdminQuizResultResponse.builder()
                        .sessionId(s.getId())
                        .userName(userRepository.findById(s.getUserId()).map(User::getName).orElse("Unknown"))
                        .score(s.getScore())
                        .status(s.getStatus())
                        .violationCount(violationRepository.countBySessionId(s.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    // ── COMPONENT HELPER MAPPERS ──────────────────────────────────────────────

    private QuizResponse mapToQuizResponse(Quiz quiz) {
        Event event = eventRepository.findById(quiz.getEventId()).orElse(null);
        return QuizResponse.builder()
                .id(quiz.getId())
                .eventId(quiz.getEventId())
                .durationMinutes(quiz.getDurationMinutes())
                .passScore(quiz.getPassScore().doubleValue())
                .randomize(quiz.getRandomize())
                .eventTitle(event != null ? event.getTitle() : "—")
                .eventStatus(event != null ? event.getStatus() : "—")
                .questionCount(questionRepository.findByQuizId(quiz.getId()).size())
                .participants(0).passRate(0.0).avgScore(0.0)
                .build();
    }

    // Same as above but also computes participation stats from completed sessions
    private QuizResponse mapToQuizResponseWithStats(Quiz quiz) {
        QuizResponse base = mapToQuizResponse(quiz);
        double pass = quiz.getPassScore().doubleValue();
        List<QuizSession> sessions = quizSessionRepository.findByQuizId(quiz.getId()).stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()) && s.getScore() != null)
                .toList();
        base.setParticipants(sessions.size());
        if (!sessions.isEmpty()) {
            double avg = sessions.stream().mapToDouble(QuizSession::getScore).average().orElse(0);
            long passed = sessions.stream().filter(s -> s.getScore() >= pass).count();
            base.setAvgScore(Math.round(avg * 10.0) / 10.0);
            base.setPassRate(Math.round(((double) passed / sessions.size()) * 1000.0) / 10.0);
        }
        return base;
    }

    private QuestionResponse mapToQuestionResponse(Question q) {
        return QuestionResponse.builder()
                .id(q.getId())
                .text(q.getQuestionText())
                .options(q.getOptions().stream().map(o -> OptionResponse.builder()
                        .id(o.getId())
                        .optionText(o.getOptionText())
                        .isCorrect(o.getIsCorrect())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
