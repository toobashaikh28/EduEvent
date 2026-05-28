package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.QuestionRequest;
import com.tooba.EduEvent.dto.request.QuizRequest;
import com.tooba.EduEvent.dto.request.SubmitQuizRequest;
import com.tooba.EduEvent.dto.request.ViolationRequest;
import com.tooba.EduEvent.dto.response.*;
import java.util.List;

public interface QuizService {
    QuizResponse createQuiz(QuizRequest request);
    QuestionResponse addQuestionToQuiz(Long quizId, QuestionRequest request);
    List<QuestionResponse> getQuestionsByQuiz(Long quizId);
    void deleteQuestion(Long questionId);
    
    QuizSessionResponse startQuiz(Long quizId, String userEmail);
    QuizResultResponse submitQuiz(Long sessionId, SubmitQuizRequest request, String userEmail);
    ViolationResponse recordViolation(Long sessionId, ViolationRequest request);
    
    List<AdminQuizResultResponse> getQuizResults(Long quizId);
    List<QuizResultResponse> getMyQuizHistory(String userEmail);
    QuizResultResponse getSessionResult(Long sessionId, String userEmail);
}