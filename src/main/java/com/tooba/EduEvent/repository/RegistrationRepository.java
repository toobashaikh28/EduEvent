package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Registration;
import org.springframework.data.jpa.repository.Query;
import com.tooba.EduEvent.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    // Counts only confirmed registrations — used for capacity check
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    // Find a specific user+event pair (any status) — used for duplicate check & cancel
    Optional<Registration> findByUserIdAndEventId(Long userId, Long eventId);

    // Admin: all registrations for an event (any status)
    List<Registration> findByEventId(Long eventId);

    // FIX #8: Added to support WaitlistServiceImpl.getWaitlistByEvent().
    // Converts the memory filter into an efficient SQL WHERE clause.
    List<Registration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);

    // User dashboard: all registrations for a user
    List<Registration> findByUserId(Long userId);

    // Finds the oldest waitlisted entry for an event — used by promoteNext()
    Optional<Registration> findFirstByEventIdAndStatusOrderByRegisteredAtAsc(
            Long eventId, RegistrationStatus status);

    // 🟢 FIXED: Added method declaration to support verification in QuizServiceImpl
    boolean existsByUserIdAndEventIdAndStatus(Long userId, Long eventId, RegistrationStatus status);

    // --- ANALYTICS QUERIES ---
    @Query("SELECT r.event.title, COUNT(r) FROM Registration r WHERE r.status = 'REGISTERED' GROUP BY r.event.title")
    List<Object[]> countRegistrationsPerEvent();

    @Query("SELECT r.event.title, COUNT(r) FROM Registration r WHERE r.status = 'CANCELLED' OR r.status = 'WAITLISTED' GROUP BY r.event.title")
    List<Object[]> countDropoutsPerEvent();
}