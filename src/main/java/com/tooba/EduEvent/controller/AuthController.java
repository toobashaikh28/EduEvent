package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.request.LoginRequest;
import com.tooba.EduEvent.dto.request.RegisterRequest;
import com.tooba.EduEvent.dto.response.UserResponse;
import com.tooba.EduEvent.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.UserRepository;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/auth") // All URLs start with this
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // Task: Build POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // Task: Build POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);

        // Load user details to include name, email, role in response
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("name",  user.getName());
        response.put("email", user.getEmail());
        response.put("role",  user.getRole());   // "USER", "ADMIN", or "JUDGE"

        return ResponseEntity.ok(response);
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