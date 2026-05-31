package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Optional<Certificate> findByCertUuid(UUID certUuid);

    List<Certificate> findAllByUserId(Long userId);
}