package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {

    // Mongo supports IgnoreCase derived queries natively
    List<Event> findByTypeIgnoreCase(String type);

    List<Event> findByStatusIgnoreCase(String status);

    List<Event> findByStartTimeBeforeAndStatus(LocalDateTime now, String status);
    List<Event> findByEndTimeBeforeAndStatus(LocalDateTime now, String status);
    List<Event> findByStartTimeAfter(LocalDateTime date);

    // Required by QuizSessionScheduler.sendHourlyQuizReminders()
    List<Event> findByStartTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end, String status);

    boolean existsByTitleIgnoreCase(String title);
}
