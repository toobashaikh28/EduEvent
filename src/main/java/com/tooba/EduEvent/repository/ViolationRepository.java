package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationRepository extends JpaRepository<Violation, Long> {
    List<Violation> findBySessionId(Long sessionId);
    int countBySessionId(Long sessionId);
}