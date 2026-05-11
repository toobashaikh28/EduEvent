package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.request.LoginRequest;
import com.tooba.EduEvent.dto.request.RegisterRequest;
import com.tooba.EduEvent.dto.response.UserResponse;
import com.tooba.EduEvent.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth") // All URLs start with this
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Task: Build POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // Task: Build POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(token);
    }

    // Task: POST /api/auth/forgot-password
    // Accepts: { "email": "user@example.com" }
    // Generates a reset token and (in production) emails it to the user
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        authService.forgotPassword(email);
        return ResponseEntity.ok("Password reset token sent. Check the server console for the DEBUG token.");
    }

    // Task: POST /api/auth/reset-password
    // Accepts: { "token": "uuid-here", "newPassword": "newpass123" }
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}