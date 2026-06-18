package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Registration;
import com.tooba.EduEvent.entity.RegistrationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends MongoRepository<Registration, String> {

    // Counts only confirmed registrations — used for capacity check
    long countByEventIdAndStatus(String eventId, RegistrationStatus status);

    // Find a specific user+event pair (any status) — used for duplicate check & cancel
    Optional<Registration> findByUserIdAndEventId(String userId, String eventId);

    // Admin: all registrations for an event (any status)
    List<Registration> findByEventId(String eventId);

    // Supports WaitlistServiceImpl.getWaitlistByEvent()
    List<Registration> findByEventIdAndStatus(String eventId, RegistrationStatus status);

    // User dashboard: all registrations for a user
    List<Registration> findByUserId(String userId);

    // Finds the oldest waitlisted entry for an event — used by promoteNext()
    Optional<Registration> findFirstByEventIdAndStatusOrderByRegisteredAtAsc(
            String eventId, RegistrationStatus status);

    boolean existsByUserIdAndEventIdAndStatus(String userId, String eventId, RegistrationStatus status);

    // Analytics: all registrations of a given status (grouping done in the service)
    List<Registration> findByStatus(RegistrationStatus status);
}
