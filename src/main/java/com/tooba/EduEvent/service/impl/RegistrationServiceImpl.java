package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service  // Singleton — Spring manages one shared instance
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository        eventRepository;
    private final UserRepository         userRepository;

    @Override
    @Transactional
    public RegistrationResponse registerUserToEvent(Long userId, Long eventId) {

        // 1. Validate Event & User existence
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 2. Prevent duplicate active registrations (allow re-register after CANCELLED)
        registrationRepository.findByUserIdAndEventId(userId, eventId).ifPresent(existing -> {
            if (existing.getStatus() != RegistrationStatus.CANCELLED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "You are already registered or waitlisted for this event.");
            }
            // Delete the old CANCELLED record so a fresh one is created below
            registrationRepository.delete(existing);
        });

        // 3. Count current REGISTERED seats (waitlisted users don't occupy a seat)
        long currentRegistrations = registrationRepository
                .countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);

        // 4. Determine status based on capacity
        RegistrationStatus targetStatus = (currentRegistrations < event.getCapacity())
                ? RegistrationStatus.REGISTERED
                : RegistrationStatus.WAITLISTED;

        // 5. Build and save using Builder pattern (Singleton service, Builder entity)
        Registration registration = Registration.builder()
                .user(user)
                .event(event)
                .status(targetStatus)
                .build();

        Registration saved = registrationRepository.save(registration);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelRegistration(Long userId, Long eventId) {
        Registration registration = registrationRepository
                .findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Registration not found."));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Registration is already cancelled.");
        }

        // Soft delete — keeps audit trail, allows re-registration later
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> getRegistrationsByEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        return registrationRepository.findByEventId(eventId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> getRegistrationsByUser(Long userId) {
        return registrationRepository.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── private mapper ────────────────────────────────────────────────────────

    private RegistrationResponse toResponse(Registration reg) {
        return RegistrationResponse.builder()
                .id(reg.getId())
                .eventId(reg.getEvent().getId())
                .eventTitle(reg.getEvent().getTitle())
                .userId(reg.getUser().getId())
                .userName(reg.getUser().getName())
                .status(reg.getStatus().name())
                .registeredAt(reg.getRegisteredAt())
                .build();
    }
}