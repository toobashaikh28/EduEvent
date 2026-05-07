package com.tooba.EduEvent.factory;

import com.tooba.EduEvent.dto.request.RegisterRequest;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.pattern.NullUser;
import com.tooba.EduEvent.pattern.UserInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserFactoryTest {

    private final UserFactory factory = new UserFactory();

    @Test
    @DisplayName("Factory should create ADMIN with encoded password")
    void testCreateAdminWithEncoding() {
        RegisterRequest req = new RegisterRequest("Tooba", "tooba@eduevent.com", "pass123");
        
        // In the test, we can mock the encoder or just check the result
        User admin = factory.createByRole("ADMIN", req);

        assertEquals("ADMIN", admin.getRole());
        // Verify password is NOT "pass123"
        assertNotEquals("pass123", admin.getPassword()); 
    }

    @Test
    @DisplayName("NullUser should return safe defaults and isNull true")
    void testNullUserBehavior() {
        // Act
        UserInterface guest = new NullUser();

        // Assert
        assertTrue(guest.isNull());
        assertEquals("Anonymous", guest.getName());
        assertEquals("GUEST", guest.getRole());
        assertEquals(-1L, guest.getId());
        
        // This proves the Null Object Pattern works: no NullPointerException
        assertDoesNotThrow(() -> guest.getEmail());
    }
}