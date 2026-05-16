package com.tooba.EduEvent;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.impl.RegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegistrationServiceImpl.
 * No Spring context, no database — Mockito only, runs instantly.
 * Scenario: event has capacity=1
 *   → First user  gets REGISTERED
 *   → Second user gets WAITLISTED
 */

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private EventRepository        eventRepository;
    @Mock private UserRepository         userRepository;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private User  user1;
    private User  user2;
    private Event event;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L).name("Alice").email("alice@test.com")
                .password("hashed").role("USER").build();

        user2 = User.builder()
                .id(2L).name("Bob").email("bob@test.com")
                .password("hashed").role("USER").build();

        // Event with only 1 seat
        event = Event.builder()
                .id(10L).title("Tiny Webinar").type("Webinar")
                .capacity(1)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .status("UPCOMING").admin(user1).build();
    }

    // Builds a Registration as it would look after being saved (has an ID and timestamp)
    private Registration savedReg(User u, Event e, RegistrationStatus status, Long id) {
        return Registration.builder()
                .id(id).user(u).event(e).status(status)
                .registeredAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("First user gets REGISTERED when 1 slot is free")
    void firstUser_getsRegistered() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(registrationRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.empty());
        // 0 seats taken → slot is free
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.REGISTERED)).thenReturn(0L);
        when(registrationRepository.save(any())).thenAnswer(inv ->
                savedReg(user1, event, ((Registration) inv.getArgument(0)).getStatus(), 100L));

        RegistrationResponse result = registrationService.registerUserToEvent(1L, 10L);

        assertEquals("REGISTERED", result.getStatus());
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    @DisplayName("Second user gets WAITLISTED when capacity is full")
    void secondUser_getsWaitlisted() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(registrationRepository.findByUserIdAndEventId(2L, 10L)).thenReturn(Optional.empty());
        // 1 seat taken → capacity (1) is full
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.REGISTERED)).thenReturn(1L);
        when(registrationRepository.save(any())).thenAnswer(inv ->
                savedReg(user2, event, ((Registration) inv.getArgument(0)).getStatus(), 101L));

        RegistrationResponse result = registrationService.registerUserToEvent(2L, 10L);

        assertEquals("WAITLISTED", result.getStatus());
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    @DisplayName("Sequential: first REGISTERED, second WAITLISTED — full scenario")
    void sequential_firstRegistered_secondWaitlisted() {
        // First call setup
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(registrationRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.empty());
        when(registrationRepository.findByUserIdAndEventId(2L, 10L)).thenReturn(Optional.empty());
        // First call: 0 seats taken. Second call: 1 seat taken.
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.REGISTERED))
                .thenReturn(0L)
                .thenReturn(1L);
        when(registrationRepository.save(any())).thenAnswer(inv ->
                savedReg(user1, event, ((Registration) inv.getArgument(0)).getStatus(), 200L));

        RegistrationResponse first  = registrationService.registerUserToEvent(1L, 10L);
        RegistrationResponse second = registrationService.registerUserToEvent(2L, 10L);

        assertEquals("REGISTERED", first.getStatus(),  "First user must be REGISTERED");
        assertEquals("WAITLISTED", second.getStatus(), "Second user must be WAITLISTED");
        verify(registrationRepository, times(2)).save(any(Registration.class));
    }
}