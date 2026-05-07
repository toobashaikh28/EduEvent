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
    @DisplayName("Should create User with ADMIN role via Factory")
    void testCreateAdminRole() {
        // Arrange
        RegisterRequest req = new RegisterRequest("Tooba", "admin@ssuet.edu", "securePass");

        // Act
        User admin = factory.createByRole("ADMIN", req);

        // Assert
        assertNotNull(admin);
        assertEquals("ADMIN", admin.getRole());
        assertEquals("Tooba", admin.getName());
        assertTrue(admin.getIsActive());
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