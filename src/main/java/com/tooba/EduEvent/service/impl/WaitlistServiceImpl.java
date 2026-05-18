package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import com.tooba.EduEvent.entity.Registration;
import com.tooba.EduEvent.entity.RegistrationStatus;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.service.NotificationService;
import com.tooba.EduEvent.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tooba.EduEvent.service.EmailService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistServiceImpl implements WaitlistService {

    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Override
    @Transactional
    public void promoteNext(Long eventId) {
        registrationRepository
                .findFirstByEventIdAndStatusOrderByRegisteredAtAsc(eventId, RegistrationStatus.WAITLISTED)
                .ifPresent(nextInLine -> {
                    nextInLine.setStatus(RegistrationStatus.REGISTERED);
                    registrationRepository.save(nextInLine);

                    Long userId = nextInLine.getUser().getId();
                    String eventTitle = nextInLine.getEvent().getTitle();

                    // Hook: notify user after waitlist promotion
                    notificationService.send(
                            userId,
                            "You're In!",
                            "A spot opened up. You have been promoted from the waitlist for: " + eventTitle
                    );

                    log.info("User ID {} promoted to REGISTERED for Event ID {}", userId, eventId);

                    String userEmail = nextInLine.getUser().getEmail();
                    if (userEmail != null && !userEmail.isEmpty()) {
                        String subject = "Good News! Waitlist Promotion for " + nextInLine.getEvent().getTitle();
                        String body = "Hi " + nextInLine.getUser().getName() + ",\n\n" +
                                "A spot has just opened up! You have been successfully promoted from the waitlist to REGISTERED for '" + 
                                nextInLine.getEvent().getTitle() + "'.\n\nWe look forward to seeing you there!\n\nBest,\nEduEvent Team";
                        
                        emailService.sendEmail(userEmail, subject, body);
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> getWaitlistByEvent(Long eventId) {
        return registrationRepository.findByEventId(eventId).stream()
                .filter(reg -> reg.getStatus() == RegistrationStatus.WAITLISTED)
                .map(this::toResponse)
                .collect(Collectors.toList());
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