package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.QuestionRequest;
import com.tooba.EduEvent.dto.request.QuizRequest;
import com.tooba.EduEvent.dto.request.SubmitQuizRequest;
import com.tooba.EduEvent.dto.request.ViolationRequest;
import com.tooba.EduEvent.dto.response.*;
import java.util.List;

public interface QuizService {
    QuizResponse createQuiz(QuizRequest request);
    List<QuizResponse> getAllQuizzes();
    QuizResponse updateQuiz(String id, QuizRequest request);
    void deleteQuiz(String id);
    QuestionResponse addQuestionToQuiz(String quizId, QuestionRequest request);
    List<QuestionResponse> getQuestionsByQuiz(String quizId);
    void deleteQuestion(String questionId);
    
    QuizSessionResponse startQuiz(String quizId, String userEmail);
    QuizResultResponse submitQuiz(String sessionId, SubmitQuizRequest request, String userEmail);
    ViolationResponse recordViolation(String sessionId, ViolationRequest request);
    
    List<AdminQuizResultResponse> getQuizResults(String quizId);
    List<QuizResultResponse> getMyQuizHistory(String userEmail);
    List<UserQuizResponse> getMyQuizzes(String userEmail);
    QuizResultResponse getSessionResult(String sessionId, String userEmail);
}