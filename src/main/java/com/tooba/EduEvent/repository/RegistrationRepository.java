package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Registration;
import com.tooba.EduEvent.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    // Counts only confirmed registrations — used for capacity check
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    // Find a specific user+event pair (any status) — used for duplicate check & cancel
    Optional<Registration> findByUserIdAndEventId(Long userId, Long eventId);

    // Admin: all registrations for an event
    List<Registration> findByEventId(Long eventId);

    // User dashboard: all registrations for a user
    List<Registration> findByUserId(Long userId);
}