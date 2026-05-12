package com.tooba.EduEvent;

import com.tooba.EduEvent.service.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class JwtSecurityTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void generatedTokenShouldBeValid() {
        String email = "admin@test.com";
        String token = jwtUtil.generateToken(email);

        assertNotNull(token, "Token should not be null");
        assertEquals(email, jwtUtil.extractEmail(token), "Extracted email should match");
        assertTrue(jwtUtil.isTokenValid(token, email), "Token should be valid");
    }

    @Test
    void tokenShouldFailForWrongEmail() {
        String token = jwtUtil.generateToken("user@test.com");
        assertFalse(jwtUtil.isTokenValid(token, "other@test.com"), "Token should be invalid for different email");
    }
}