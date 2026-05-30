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

    // FIX 2: Load all ONGOING sessions and let the scheduler filter by duration in Java.
    // FUNCTION('DATEADD',...) is SQL Server-specific and breaks on H2 (used in tests).
    // The scheduler already runs every 5 minutes so in-memory filtering is fine.
    @Query("SELECT s FROM QuizSession s WHERE s.status = 'ONGOING'")
    List<QuizSession> findTrulyExpiredSessions(@Param("now") LocalDateTime now);

    // Old query kept for reference — not used
    @Query("SELECT s FROM QuizSession s WHERE s.status = 'ONGOING' AND s.startTime < :cutoff")
    List<QuizSession> findExpiredSessions(@Param("cutoff") LocalDateTime cutoff);

    List<QuizSession> findByQuizId(Long quizId);

    List<QuizSession> findByStatus(String status);

    List<QuizSession> findByUserId(Long userId);
}