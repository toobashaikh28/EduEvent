package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import com.tooba.EduEvent.entity.Registration;
import com.tooba.EduEvent.entity.RegistrationStatus;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service // Singleton
@RequiredArgsConstructor
@Slf4j
public class WaitlistServiceImpl implements WaitlistService {

    private final RegistrationRepository registrationRepository;

    @Override
    @Transactional
    public void promoteNext(Long eventId) {
        // Find waitlist entry with lowest position number (earliest timestamp)
        registrationRepository.findFirstByEventIdAndStatusOrderByRegisteredAtAsc(eventId, RegistrationStatus.WAITLISTED)
            .ifPresent(nextInLine -> {
                nextInLine.setStatus(RegistrationStatus.REGISTERED);
                registrationRepository.save(nextInLine);
                log.info("User ID {} has been promoted to REGISTERED for Event ID {}", nextInLine.getUser().getId(), eventId);
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