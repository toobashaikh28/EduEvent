package com.tooba.EduEvent.repository;
 
import com.tooba.EduEvent.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.time.LocalDateTime;
import java.util.List;
 
public interface EventRepository extends JpaRepository<Event, Long> {
 
    // Force LOWER() on both sides in the SQL itself — bypasses SQL Server collation completely
    @Query("SELECT e FROM Event e WHERE LOWER(e.type) = LOWER(:type)")
    List<Event> findByTypeIgnoreCase(@Param("type") String type);
 
    @Query("SELECT e FROM Event e WHERE LOWER(e.status) = LOWER(:status)")
    List<Event> findByStatusIgnoreCase(@Param("status") String status);
 
    List<Event> findByStartTimeBeforeAndStatus(LocalDateTime now, String status);
    List<Event> findByEndTimeBeforeAndStatus(LocalDateTime now, String status);
    List<Event> findByStartTimeAfter(LocalDateTime date);

    // ── ADDED TO FIX COMPILATION ERRORS ──────────────────────────────────────
    
    // Required by QuizSessionScheduler.sendHourlyQuizReminders()
    List<Event> findByStartTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end, String status);

    boolean existsByTitleIgnoreCase(String title);
}