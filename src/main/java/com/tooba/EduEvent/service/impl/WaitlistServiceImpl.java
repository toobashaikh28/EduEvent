package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.Registration;
import com.tooba.EduEvent.entity.RegistrationStatus;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.EventRepository;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.EmailService;
import com.tooba.EduEvent.service.NotificationService;
import com.tooba.EduEvent.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistServiceImpl implements WaitlistService {

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Override
    public void promoteNext(String eventId) {
        registrationRepository
                .findFirstByEventIdAndStatusOrderByRegisteredAtAsc(eventId, RegistrationStatus.WAITLISTED)
                .ifPresent(nextInLine -> {
                    nextInLine.setStatus(RegistrationStatus.REGISTERED);
                    registrationRepository.save(nextInLine);

                    String userId = nextInLine.getUserId();
                    User user = userRepository.findById(userId).orElse(null);
                    String eventTitle = eventRepository.findById(eventId).map(Event::getTitle).orElse("the event");

                    notificationService.send(
                            userId,
                            "You're In!",
                            "A spot opened up. You have been promoted from the waitlist for: " + eventTitle
                    );

                    log.info("User ID {} promoted to REGISTERED for Event ID {}", userId, eventId);

                    if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                        String subject = "Good News! Waitlist Promotion for " + eventTitle;
                        String body = "Hi " + user.getName() + ",\n\n"
                                + "A spot has just opened up! You have been promoted from the waitlist "
                                + "to REGISTERED for '" + eventTitle + "'."
                                + "\n\nWe look forward to seeing you there!\n\nBest,\nEduEvent Team";
                        emailService.sendEmail(user.getEmail(), subject, body);
                    }
                });
    }

    @Override
    public List<RegistrationResponse> getWaitlistByEvent(String eventId) {
        return registrationRepository
                .findByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private RegistrationResponse toResponse(Registration reg) {
        String eventTitle = eventRepository.findById(reg.getEventId()).map(Event::getTitle).orElse(null);
        String userName = userRepository.findById(reg.getUserId()).map(User::getName).orElse(null);
        return RegistrationResponse.builder()
                .id(reg.getId())
                .eventId(reg.getEventId())
                .eventTitle(eventTitle)
                .userId(reg.getUserId())
                .userName(userName)
                .status(reg.getStatus().name())
                .registeredAt(reg.getRegisteredAt())
                .build();
    }
}
