package com.tooba.EduEvent;

import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.EventRepository;
import com.tooba.EduEvent.scheduler.EventScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventScheduler.
 * Uses Mockito only — no database, no Spring context, runs instantly.
 * Tests the exact scheduler methods in your EventScheduler class.
 */

@ExtendWith(MockitoExtension.class)
class EventSchedulerTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventScheduler eventScheduler;

    private User mockAdmin;

    @BeforeEach
    void setUp() {
        mockAdmin = User.builder()
                .id(1L)
                .name("Tooba")
                .email("tooba@eduevent.com")
                .password("password123")
                .role("ADMIN")
                .build();
    }

    // UPCOMING → LIVE Tests
    @Test
    @DisplayName("Should mark UPCOMING event as LIVE when start_time has passed")
    void shouldMarkUpcomingEventAsLive() {
        // ARRANGE: event that started 2 minutes ago, still UPCOMING
        Event event = Event.builder()
                .title("Test Webinar")
                .type("Webinar")
                .startTime(LocalDateTime.now().minusMinutes(2))  // started 2 min ago
                .endTime(LocalDateTime.now().plusHours(1))
                .status("UPCOMING")
                .admin(mockAdmin)
                .build();

        when(eventRepository.findByStartTimeBeforeAndStatus(any(LocalDateTime.class), eq("UPCOMING")))
                .thenReturn(List.of(event));

        // ACT: manually trigger the scheduler (no need to wait 10 minutes)
        eventScheduler.updateUpcomingToLive();

        // ASSERT: status must be LIVE
        assertEquals("LIVE", event.getStatus(), "Event status should be LIVE after scheduler runs");
        verify(eventRepository).saveAll(List.of(event));
    }

    @Test
    @DisplayName("Should NOT change status of UPCOMING event whose start_time is in the future")
    void shouldNotMarkFutureEventAsLive() {
        // ARRANGE: event that starts 5 minutes from now — should NOT be touched
        when(eventRepository.findByStartTimeBeforeAndStatus(any(LocalDateTime.class), eq("UPCOMING")))
                .thenReturn(Collections.emptyList()); // repo returns nothing

        // ACT
        eventScheduler.updateUpcomingToLive();

        // ASSERT: saveAll called with empty list — no status change
        verify(eventRepository).saveAll(Collections.emptyList());
    }

    @Test
    @DisplayName("Should mark multiple UPCOMING events as LIVE in one scheduler run")
    void shouldMarkMultipleEventsAsLive() {
        // ARRANGE: two events that have both already started
        Event event1 = Event.builder()
                .title("Event One")
                .type("Conference")
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now().plusHours(2))
                .status("UPCOMING")
                .admin(mockAdmin)
                .build();

        Event event2 = Event.builder()
                .title("Event Two")
                .type("Hackathon")
                .startTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now().plusHours(3))
                .status("UPCOMING")
                .admin(mockAdmin)
                .build();

        when(eventRepository.findByStartTimeBeforeAndStatus(any(LocalDateTime.class), eq("UPCOMING")))
                .thenReturn(List.of(event1, event2));

        // ACT
        eventScheduler.updateUpcomingToLive();

        // ASSERT: both events flipped to LIVE
        assertEquals("LIVE", event1.getStatus());
        assertEquals("LIVE", event2.getStatus());

        // Capture what was passed to saveAll to verify both events included
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    // LIVE → COMPLETED Tests

    @Test
    @DisplayName("Should mark LIVE event as COMPLETED when end_time has passed")
    void shouldMarkLiveEventAsCompleted() {
        // ARRANGE: event that ended 10 minutes ago, still LIVE
        Event event = Event.builder()
                .title("Finished Quiz")
                .type("Quiz")
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusMinutes(10)) // ended 10 min ago
                .status("LIVE")
                .admin(mockAdmin)
                .build();

        when(eventRepository.findByEndTimeBeforeAndStatus(any(LocalDateTime.class), eq("LIVE")))
                .thenReturn(List.of(event));

        // ACT
        eventScheduler.updateLiveToCompleted();

        // ASSERT
        assertEquals("COMPLETED", event.getStatus(), "Event status should be COMPLETED after scheduler runs");
        verify(eventRepository).saveAll(List.of(event));
    }

    @Test
    @DisplayName("Should NOT mark LIVE event as COMPLETED if end_time is in the future")
    void shouldNotMarkOngoingEventAsCompleted() {
        // ARRANGE: event still running — repo returns nothing
        when(eventRepository.findByEndTimeBeforeAndStatus(any(LocalDateTime.class), eq("LIVE")))
                .thenReturn(Collections.emptyList());

        // ACT
        eventScheduler.updateLiveToCompleted();

        // ASSERT: saveAll called but with no events
        verify(eventRepository).saveAll(Collections.emptyList());
    }

    // Full lifecycle: UPCOMING → LIVE → COMPLETED

    @Test
    @DisplayName("Full lifecycle: UPCOMING → LIVE → COMPLETED through two scheduler runs")
    void shouldTransitionThroughFullLifecycle() {
        // ARRANGE: event in UPCOMING state
        Event event = Event.builder()
                .title("Lifecycle Event")
                .type("Webinar")
                .startTime(LocalDateTime.now().minusMinutes(2))
                .endTime(LocalDateTime.now().minusMinutes(1)) // already ended too
                .status("UPCOMING")
                .admin(mockAdmin)
                .build();

        // Step 1: scheduler run → UPCOMING to LIVE
        when(eventRepository.findByStartTimeBeforeAndStatus(any(LocalDateTime.class), eq("UPCOMING")))
                .thenReturn(List.of(event));

        eventScheduler.updateUpcomingToLive();
        assertEquals("LIVE", event.getStatus(), "After first run: should be LIVE");

        // Step 2: scheduler run → LIVE to COMPLETED
        when(eventRepository.findByEndTimeBeforeAndStatus(any(LocalDateTime.class), eq("LIVE")))
                .thenReturn(List.of(event));

        eventScheduler.updateLiveToCompleted();
        assertEquals("COMPLETED", event.getStatus(), "After second run: should be COMPLETED");

        verify(eventRepository, times(1)).saveAll(argThat(list -> ((List<?>) list).size() == 1));
    }
}