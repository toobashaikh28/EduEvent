package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    // Check if user already has an active or passed session for this quiz
    @Query("SELECT s FROM QuizSession s WHERE s.quiz.id = :quizId AND s.user.id = :userId AND s.status IN ('ONGOING', 'COMPLETED')")
    Optional<QuizSession> findActiveOrCompletedSession(@Param("quizId") Long quizId, @Param("userId") Long userId);

    // Auto-submit scheduler: find ONGOING sessions whose time has expired
    @Query("SELECT s FROM QuizSession s WHERE s.status = 'ONGOING' AND s.startTime < :cutoff")
    List<QuizSession> findExpiredSessions(@Param("cutoff") LocalDateTime cutoff);

    // Admin results view: all sessions for a quiz
    List<QuizSession> findByQuizId(Long quizId);

    // ── ADDED TO FIX COMPILATION ERRORS ──────────────────────────────────────
    
    // Required by QuizSessionScheduler.autoSubmitExpiredSessions()
    List<QuizSession> findByStatus(String status);

    // Required by QuizServiceImpl.getMyQuizHistory()
    List<QuizSession> findByUserId(Long userId);
}