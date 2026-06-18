package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Certificate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends MongoRepository<Certificate, String> {

    boolean existsByUserIdAndEventId(String userId, String eventId);

    Optional<Certificate> findByCertUuid(UUID certUuid);

    List<Certificate> findAllByUserId(String userId);
}
