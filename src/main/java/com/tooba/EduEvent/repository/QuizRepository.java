package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends MongoRepository<Quiz, String> {

    boolean existsByEventId(String eventId);

    Optional<Quiz> findByEventId(String eventId);

    // Find quizzes for events the user is registered for
    List<Quiz> findByEventIdIn(List<String> eventIds);
}
