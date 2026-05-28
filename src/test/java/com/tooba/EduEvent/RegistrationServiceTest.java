package com.tooba.EduEvent;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.EmailService;
import com.tooba.EduEvent.service.NotificationService;
import com.tooba.EduEvent.service.WaitlistService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegistrationServiceImpl.
 * No Spring context, no database — Mockito only, runs instantly.
 *
 * FIX 1: Added 3 missing @Mock fields:
 *   - WaitlistService    — called by cancelRegistration() to promote next user
 *   - NotificationService — called after every registration to send in-app notification
 *   - EmailService        — called after every registration to send confirmation email
 *
 * Without these mocks, Mockito's @InjectMocks cannot inject the full constructor,
 * causing NullPointerException when the service tries to call notificationService.send()
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    // ─── Repositories ─────────────────────────────────────────────────────────
    @Mock private RegistrationRepository registrationRepository;
    @Mock private EventRepository        eventRepository;
    @Mock private UserRepository         userRepository;

    // ─── FIX 1: These 3 mocks were missing — caused NullPointerException ──────
    @Mock private WaitlistService     waitlistService;      // FIX 1: was missing
    @Mock private NotificationService notificationService;  // FIX 1: was missing
    @Mock private EmailService        emailService;         // FIX 1: was missing

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    // ─── Test Data ─────────────────────────────────────────────────────────────
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

    // Helper: builds a saved Registration with an ID and timestamp
    private Registration savedReg(User u, Event e, RegistrationStatus status, Long id) {
        return Registration.builder()
                .id(id).user(u).event(e).status(status)
                .registeredAt(LocalDateTime.now()).build();
    }

    // ─── Test 1 ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("First user gets REGISTERED when 1 slot is free")
    void firstUser_getsRegistered() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(registrationRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.empty());
        // 0 seats taken → slot is free
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.REGISTERED))
                .thenReturn(0L);
        when(registrationRepository.save(any())).thenAnswer(inv ->
                savedReg(user1, event, ((Registration) inv.getArgument(0)).getStatus(), 100L));

        // FIX 1: notificationService.send() and emailService.sendEmail() are called inside
        // registerUserToEvent() — Mockito will call the mock versions (do nothing by default)
        // Without the @Mock fields above, these calls throw NullPointerException

        RegistrationResponse result = registrationService.registerUserToEvent(1L, 10L);

        assertEquals("REGISTERED", result.getStatus());
        verify(registrationRepository).save(any(Registration.class));

        // FIX 1: Verify notification and email were sent after registration
        verify(notificationService).send(eq(1L), anyString(), anyString());
        verify(emailService).sendEmail(eq("alice@test.com"), anyString(), anyString());
    }

    // ─── Test 2 ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Second user gets WAITLISTED when capacity is full")
    void secondUser_getsWaitlisted() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(registrationRepository.findByUserIdAndEventId(2L, 10L)).thenReturn(Optional.empty());
        // 1 seat taken → capacity (1) is full
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.REGISTERED))
                .thenReturn(1L);
        when(registrationRepository.save(any())).thenAnswer(inv ->
                savedReg(user2, event, ((Registration) inv.getArgument(0)).getStatus(), 101L));

        RegistrationResponse result = registrationService.registerUserToEvent(2L, 10L);

        assertEquals("WAITLISTED", result.getStatus());
        verify(registrationRepository).save(any(Registration.class));

        // FIX 1: Verify waitlist notification was sent
        verify(notificationService).send(eq(2L), anyString(), anyString());
        verify(emailService).sendEmail(eq("bob@test.com"), anyString(), anyString());
    }

    // ─── Test 3 ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Sequential: first REGISTERED, second WAITLISTED — full scenario")
    void sequential_firstRegistered_secondWaitlisted() {
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

        // FIX 1: Both users should receive notifications and emails
        verify(notificationService, times(2)).send(anyLong(), anyString(), anyString());
        verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
    }

    // ─── Test 4 (New) ──────────────────────────────────────────────────────────
    @Test
    @DisplayName("Cancel registration calls waitlistService.promoteNext()")
    void cancelRegistration_callsPromoteNext() {
        // Build an existing REGISTERED registration
        Registration existing = savedReg(user1, event, RegistrationStatus.REGISTERED, 300L);

        when(registrationRepository.findByUserIdAndEventId(1L, 10L))
                .thenReturn(Optional.of(existing));
        when(registrationRepository.save(any())).thenReturn(existing);

        registrationService.cancelRegistration(1L, 10L);

        // After cancelling a REGISTERED user, promoteNext should be called
        verify(waitlistService).promoteNext(10L);
        verify(registrationRepository).save(any(Registration.class));
    }

    // ─── Test 5 (New) ──────────────────────────────────────────────────────────
    @Test
    @DisplayName("Cancel WAITLISTED registration does NOT call promoteNext()")
    void cancelWaitlisted_doesNotCallPromoteNext() {
        // A WAITLISTED user cancels — no need to promote anyone
        Registration waitlisted = savedReg(user2, event, RegistrationStatus.WAITLISTED, 400L);

        when(registrationRepository.findByUserIdAndEventId(2L, 10L))
                .thenReturn(Optional.of(waitlisted));
        when(registrationRepository.save(any())).thenReturn(waitlisted);

        registrationService.cancelRegistration(2L, 10L);

        // promoteNext should NOT be called for waitlisted cancellations
        verify(waitlistService, never()).promoteNext(anyLong());
    }

    // ─── Test 6 (New) ──────────────────────────────────────────────────────────
    @Test
    @DisplayName("Duplicate registration throws CONFLICT exception")
    void duplicateRegistration_throwsConflict() {
        // User already has an active REGISTERED entry
        Registration existing = savedReg(user1, event, RegistrationStatus.REGISTERED, 500L);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(registrationRepository.findByUserIdAndEventId(1L, 10L))
                .thenReturn(Optional.of(existing));

        // Should throw 409 CONFLICT
        org.springframework.web.server.ResponseStatusException ex =
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> registrationService.registerUserToEvent(1L, 10L));

        assertEquals(409, ex.getStatusCode().value());
    }
}