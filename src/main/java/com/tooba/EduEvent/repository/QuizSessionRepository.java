package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.QuizSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QuizSessionRepository extends MongoRepository<QuizSession, String> {

    // Check if user already has an active or completed session for this quiz
    Optional<QuizSession> findFirstByQuizIdAndUserIdAndStatusIn(String quizId, String userId, List<String> statuses);

    List<QuizSession> findByQuizId(String quizId);

    List<QuizSession> findByStatus(String status);

    List<QuizSession> findByUserId(String userId);
}
