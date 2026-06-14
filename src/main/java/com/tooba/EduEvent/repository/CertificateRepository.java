package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // 1. Added the import here

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Optional<Certificate> findByCertUuid(UUID certUuid);

    List<Certificate> findAllByUserId(Long userId);

    // --- ANALYTICS QUERIES ---
    @Query("SELECT c.event.title, COUNT(c) FROM Certificate c GROUP BY c.event.title")
    List<Object[]> countCertificatesPerEvent();
}