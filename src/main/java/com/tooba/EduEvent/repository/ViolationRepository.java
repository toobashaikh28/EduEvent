package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Violation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ViolationRepository extends MongoRepository<Violation, String> {
    List<Violation> findBySessionId(String sessionId);
    int countBySessionId(String sessionId);
}
