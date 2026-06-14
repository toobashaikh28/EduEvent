package com.tooba.EduEvent.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    @DisplayName("Should build User entity correctly without NullPointerException")
    void testUserBuilder() {
        // 1. Arrange & Act: Use the Builder pattern as specified in your task
        User user = User.builder()
                .name("Tooba")
                .email("Tooba@gmail.com")
                .role("ADMIN")
                .password("encoded_password_123")
                .build();

        // 2. Assert: Verify the fields match
        assertNotNull(user, "User object should not be null");
        assertEquals("Tooba", user.getName());
        assertEquals("Tooba@gmail.com", user.getEmail());
        assertEquals("ADMIN", user.getRole());

        // 3. Verify @Builder.Default logic
        // This is crucial: if @Builder.Default isn't used in the Entity, 
        // isActive would be null here.
        assertNotNull(user.getIsActive(), "isActive should not be null when using Builder");
        assertTrue(user.getIsActive(), "isActive should default to true");

        // 4. Verify optional fields are null but don't break the object
        assertNull(user.getPhoto());
        assertNull(user.getBio());
        
        System.out.println("Test Passed: User entity created successfully for: " + user.getName());
    }

    @Test
    @DisplayName("Should allow updating fields via Lombok setters")
    void testUserSetters() {
        User user = new User();
        user.setName("Tooba");
        
        assertEquals("Tooba", user.getName());
    }
}