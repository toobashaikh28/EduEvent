package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.EmailService;
import com.tooba.EduEvent.service.NotificationService;
import com.tooba.EduEvent.service.RegistrationService;
import com.tooba.EduEvent.mediator.NotificationMediator;
import com.tooba.EduEvent.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final WaitlistService waitlistService;
    private final NotificationService notificationService;
    private final NotificationMediator notificationMediator;
    private final EmailService emailService;

    @Override
    @Transactional
    public RegistrationResponse registerUserToEvent(Long userId, Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        registrationRepository.findByUserIdAndEventId(userId, eventId).ifPresent(existing -> {
            if (existing.getStatus() != RegistrationStatus.CANCELLED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "You are already registered or waitlisted for this event.");
            }
            // FIX #6: deleteById + flush ensures the old cancelled row is physically removed
            // from the DB before we save a new one with the same (user_id, event_id) pair.
            // Without flush(), Hibernate may batch the delete and insert in the wrong order,
            // hitting the UNIQUE constraint on (user_id, event_id) in the registrations table.
            registrationRepository.deleteById(existing.getId());
            registrationRepository.flush();
        });

        long currentRegistrations = registrationRepository
                .countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);

        RegistrationStatus targetStatus = (currentRegistrations < event.getCapacity())
                ? RegistrationStatus.REGISTERED
                : RegistrationStatus.WAITLISTED;

        Registration registration = Registration.builder()
                .user(user)
                .event(event)
                .status(targetStatus)
                .build();

        Registration saved = registrationRepository.save(registration);

        // Send confirmation email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            String subject = "EduEvent Update: Registration Processed";
            String body = "Hi " + user.getName() + ",\n\n"
                    + (targetStatus == RegistrationStatus.REGISTERED
                            ? "Your seat for '" + event.getTitle() + "' is CONFIRMED!"
                            : "The event is full. You have been placed on the WAITLIST for '"
                              + event.getTitle() + "'.")
                    + "\n\nEvent details are accessible in your application dashboard."
                    + "\n\nBest,\nEduEvent Team";
            emailService.sendEmail(user.getEmail(), subject, body);
        }

        // ── MEDIATOR: notify user of registration outcome ────────────────────
        if (targetStatus == RegistrationStatus.REGISTERED) {
            notificationMediator.notify(
                this,
                "REGISTRATION",
                userId,
                "You are registered for '" + event.getTitle() + "'. See you there!"
            );
        } else {
            notificationMediator.notify(
                this,
                "WAITLISTED",
                userId,
                "The event is full. You are on the waitlist for: " + event.getTitle()
            );
        }
        // ─────────────────────────────────────────────────────────────────────

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

        RegistrationStatus previousStatus = registration.getStatus();

        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        // Only promote from waitlist if a confirmed seat was freed
        if (previousStatus == RegistrationStatus.REGISTERED) {
            waitlistService.promoteNext(eventId);
        }
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