package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.request.QuestionRequest;
import com.tooba.EduEvent.dto.request.QuizRequest;
import com.tooba.EduEvent.dto.request.SubmitQuizRequest;
import com.tooba.EduEvent.dto.request.ViolationRequest;
import com.tooba.EduEvent.dto.response.*;
import com.tooba.EduEvent.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuizResponse> createQuiz(@Valid @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.createQuiz(request));
    }

    // Admin: list all quizzes (with participation stats)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<QuizResponse>> getAllQuizzes() {
        return ResponseEntity.ok(quizService.getAllQuizzes());
    }

    // Participant: quizzes for the events I'm registered for, with my attempt status
    @GetMapping("/my-quizzes")
    public ResponseEntity<List<UserQuizResponse>> getMyQuizzes(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(quizService.getMyQuizzes(user.getUsername()));
    }

    // Admin: edit quiz settings (duration / pass score / randomize)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuizResponse> updateQuiz(@PathVariable String id, @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.updateQuiz(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteQuiz(@PathVariable String id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok("Quiz deleted successfully.");
    }

    @PostMapping("/{id}/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuestionResponse> addQuestion(
            @PathVariable String id,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(quizService.addQuestionToQuiz(id, request));
    }

    @GetMapping("/{id}/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<QuestionResponse>> getQuestions(@PathVariable String id) {
        return ResponseEntity.ok(quizService.getQuestionsByQuiz(id));
    }

    @DeleteMapping("/question/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteQuestion(@PathVariable String id) {
        quizService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    // Start quiz — any authenticated user
    @PostMapping("/{id}/start")
    public ResponseEntity<QuizSessionResponse> startQuiz(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(quizService.startQuiz(id, userDetails.getUsername()));
    }

    // Submit quiz
    @PostMapping("/session/{id}/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable String id,
            @RequestBody SubmitQuizRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Bug 3 fix: pass the caller's email so the service can verify session ownership
        return ResponseEntity.ok(quizService.submitQuiz(id, request, userDetails.getUsername()));
    }

    // Record violation
    @PostMapping("/session/{id}/violation")
    public ResponseEntity<ViolationResponse> recordViolation(
            @PathVariable String id,
            @Valid @RequestBody ViolationRequest request) {
        return ResponseEntity.ok(quizService.recordViolation(id, request));
    }

    // Admin: view all results
    @GetMapping("/{id}/results")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminQuizResultResponse>> getResults(@PathVariable String id) {
        return ResponseEntity.ok(quizService.getQuizResults(id));
    }
}