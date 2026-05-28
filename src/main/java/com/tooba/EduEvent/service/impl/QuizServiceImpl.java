package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.request.QuestionRequest;
import com.tooba.EduEvent.dto.request.QuizRequest;
import com.tooba.EduEvent.dto.request.SubmitQuizRequest;
import com.tooba.EduEvent.dto.request.ViolationRequest;
import com.tooba.EduEvent.dto.response.*;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final ViolationRepository violationRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;

    // ── CREATE QUIZ ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found with id: " + request.getEventId()));

        Quiz quiz = Quiz.builder()
                .event(event)
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 30)
                .passScore(request.getPassScore() != null ? request.getPassScore() : new java.math.BigDecimal("50.00"))
                .randomize(request.getRandomize() != null ? request.getRandomize() : true)
                .build();
        return mapToQuizResponse(quizRepository.save(quiz));
    }

    @Override
    @Transactional
    public QuestionResponse addQuestionToQuiz(Long quizId, QuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        Question question = Question.builder()
                .quiz(quiz)
                .questionText(request.getText())
                .build();
        Question saved = questionRepository.save(question);

        List<Option> options = request.getOptions().stream().map(o ->
                Option.builder()
                        .question(saved)
                        .optionText(o.getOptionText())
                        .isCorrect(o.isCorrect())
                        .build()
        ).collect(Collectors.toList());
        optionRepository.saveAll(options);
        saved.setOptions(options);

        return mapToQuestionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByQuiz(Long quizId) {
        quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        return questionRepository.findByQuizId(quizId).stream()
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        questionRepository.deleteById(questionId);
    }

    // ── START QUIZ ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizSessionResponse startQuiz(Long quizId, String userEmail) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 🛡️ Guard 1: Block initialization if host event is not LIVE
        if (quiz.getEvent() == null || !"LIVE".equals(quiz.getEvent().getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Quiz cannot be started because the associated event is not LIVE.");
        }

        // 🛡️ Guard 2: Verify application lifecycle enrollment status
        boolean isRegistered = registrationRepository
                .existsByUserIdAndEventIdAndStatus(user.getId(), quiz.getEvent().getId(), RegistrationStatus.REGISTERED);
        if (!isRegistered) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You must be registered for this event to take the quiz.");
        }

        // 🛡️ Guard 3: Block multiple concurrent tracks or historical passed instances
        quizSessionRepository.findActiveOrCompletedSession(quizId, user.getId())
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

        // Instantiate database trace record
        QuizSession session = QuizSession.builder()
                .quiz(quiz)
                .user(user)
                .startTime(LocalDateTime.now())
                .status("ONGOING")
                .build();
        QuizSession savedSession = quizSessionRepository.save(session);

        // Map and prepare question array listings
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
    @Transactional
    public QuizResultResponse submitQuiz(Long sessionId, SubmitQuizRequest request, String userEmail) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // Bug 3 fix: verify the session belongs to the caller
        if (!session.getUser().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access Denied: You can only submit your own quiz session.");
        }

        // 🛡️ Guard 4: Block grading attempts if security engine terminated the track
        if ("INVALIDATED".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "SessionInvalidated: Submission rejected. This trace has been terminated due to proctoring breaches.");
        }

        if (!"ONGOING".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This session is already " + session.getStatus());
        }

        Quiz quiz = session.getQuiz();
        long minutesElapsed = java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
        if (minutesElapsed > quiz.getDurationMinutes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "TimeLimitExceeded: Time limit exceeded. Your session has expired.");
        }

        // Processing grade scores
        Map<Long, Long> answers = request.getAnswers();
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        int totalCount = questions.size();
        int correctCount = 0;

        for (Question q : questions) {
            Long selectedOptionId = answers.get(q.getId());
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
    @Transactional(readOnly = true)
    public List<QuizResultResponse> getMyQuizHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return quizSessionRepository.findByUserId(user.getId()).stream()
                .map(s -> QuizResultResponse.builder()
                        .score(s.getScore())
                        .passScore(s.getQuiz().getPassScore().doubleValue())
                        .isPassed(s.getScore() != null && s.getScore() >= s.getQuiz().getPassScore().doubleValue())
                        .correctCount(0) // Aggregated historic summaries simplify item matching tracking counts
                        .totalCount(0)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResultResponse getSessionResult(Long sessionId, String userEmail) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUser().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Resource isolation breach.");
        }

        boolean isPassed = session.getScore() != null && session.getScore() >= session.getQuiz().getPassScore().doubleValue();

        return QuizResultResponse.builder()
                .score(session.getScore())
                .passScore(session.getQuiz().getPassScore().doubleValue())
                .isPassed(isPassed)
                .build();
    }

    // ── VIOLATION PROCTORING CONTROL ──────────────────────────────────────────

    @Override
    @Transactional
    public ViolationResponse recordViolation(Long sessionId, ViolationRequest request) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!"ONGOING".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot record violation for a session that is " + session.getStatus());
        }

        Violation violation = Violation.builder()
                .session(session)
                .type(request.getType())
                .build();
        Violation saved = violationRepository.save(violation);

        int totalCount = violationRepository.countBySessionId(sessionId);

        if (totalCount > 3) {
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
    @Transactional(readOnly = true)
    public List<AdminQuizResultResponse> getQuizResults(Long quizId) {
        return quizSessionRepository.findByQuizId(quizId).stream()
                .map(s -> AdminQuizResultResponse.builder()
                        .sessionId(s.getId())
                        .userName(s.getUser().getName())
                        .score(s.getScore())
                        .status(s.getStatus())
                        .violationCount(violationRepository.countBySessionId(s.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    // ── COMPONENT HELPER MAPPERS ──────────────────────────────────────────────

    private QuizResponse mapToQuizResponse(Quiz quiz) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .eventId(quiz.getEvent().getId())
                .durationMinutes(quiz.getDurationMinutes())
                .passScore(quiz.getPassScore().doubleValue())
                .randomize(quiz.getRandomize())
                .build();
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