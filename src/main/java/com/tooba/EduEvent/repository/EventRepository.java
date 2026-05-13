package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    // Spring Data JPA automatically writes the SQL for these:
    List<Event> findByType(String type);
    List<Event> findByStatus(String status);
    List<Event> findByStartTimeAfter(LocalDateTime date);
}