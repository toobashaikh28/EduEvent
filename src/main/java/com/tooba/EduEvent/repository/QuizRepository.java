package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.Quiz;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    boolean existsByEvent(Event event);

    // --- ANALYTICS QUERIES ---
    @Query("SELECT q.quiz.id, AVG(q.score), " +
           "CAST(SUM(CASE WHEN q.score >= q.quiz.passScore THEN 1 ELSE 0 END) AS double) / COUNT(q) * 100 " +
           "FROM QuizSession q WHERE q.status = 'COMPLETED' GROUP BY q.quiz.id")
    List<Object[]> getQuizStats();

    // Add this to find quizzes for events the user is registered for
    List<Quiz> findByEventIdIn(List<Long> eventIds);
}