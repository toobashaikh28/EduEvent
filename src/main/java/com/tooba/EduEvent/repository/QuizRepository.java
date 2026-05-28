package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    boolean existsByEvent(Event event);
}