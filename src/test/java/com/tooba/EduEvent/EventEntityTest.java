package com.tooba.EduEvent.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class EventEntityTest {

    @Test
    @DisplayName("Should build Event entity with Admin relationship without NPE")
    void testEventBuilder() {
        // 1. Create a mock admin user first
        User mockAdmin = User.builder()
                .id(1L)
                .name("Tooba")
                .email("tooba@eduevent.com")
                .build();

        // 2. Act: Build the event using the builder pattern
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 12, 0);

        Event event = Event.builder()
                .title("Spring Boot Masters")
                .type("Webinar")
                .description("A deep dive into Spring Boot 3.x")
                .capacity(50)
                .startTime(start)
                .endTime(end)
                .admin(mockAdmin) // Associating the admin
                .build();

        // 3. Assert: Verify all fields and relationships
        assertNotNull(event, "Event object should not be null");
        assertEquals("Spring Boot Masters", event.getTitle());
        
        // Verify relationship logic
        assertNotNull(event.getAdmin(), "Admin relationship should not be null");
        assertEquals("Tooba", event.getAdmin().getName());

        // Verify @Builder.Default logic for status
        assertNotNull(event.getStatus(), "Status should not be null");
        assertEquals("UPCOMING", event.getStatus());

        System.out.println("Event and Relationship Test Passed!");
    }
}