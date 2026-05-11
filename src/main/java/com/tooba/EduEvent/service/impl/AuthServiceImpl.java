package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.service.security.JwtUtil;
import com.tooba.EduEvent.service.AuthService;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.factory.UserFactory;
import com.tooba.EduEvent.pattern.NullUser;
import com.tooba.EduEvent.pattern.UserInterface;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.dto.request.LoginRequest;
import com.tooba.EduEvent.dto.request.RegisterRequest;
import com.tooba.EduEvent.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor // Constructor Injection for Singleton efficiency
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public UserResponse register(RegisterRequest request) {
        // 1. Use Factory to create the user entity based on role
        // We'll default to "USER" or pass it from the request if needed
        // Use role from request, default to USER if not provided
        String role = (request.getRole() != null && !request.getRole().isBlank()) ? request.getRole() : "USER";
        User user = userFactory.createByRole(role, request);
        
        // 2. Save to database
        User savedUser = userRepository.save(user);
        
        // 3. Map to DTO (Manual mapping for now to ensure no password leak)
        return UserResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    public String login(LoginRequest request) {
        // Null Object Pattern: findByEmail returns Optional
        // If email not found, use NullUser instead of throwing immediately
        UserInterface userInterface = userRepository.findByEmail(request.getEmail())
                .<UserInterface>map(u -> u)   // real User → cast to UserInterface
                .orElse(new NullUser());      // not found → safe NullUser object

        // NullUser.isNull() == true means email didn't exist → return 401
        if (userInterface.isNull()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Safe to cast — we confirmed it's a real User
        User user = (User) userInterface;

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return jwtUtil.generateToken(user.getEmail());
    }

    @Override
    public void forgotPassword(String email) {
        // 1. Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with this email"));

        // 2. Generate a unique random token (using UUID)
        String resetToken = java.util.UUID.randomUUID().toString();
        
        // 3. Save token to user entity
        user.setResetToken(resetToken);
        userRepository.save(user);

        // 4. In a real app, you'd email this token. For now, print it to the console
        System.out.println("DEBUG: Password reset token for " + email + " is: " + resetToken);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // 1. Find user by the reset token
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        // 2. Encode the new password and update the user
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // 3. Clear the reset token so it can't be used again
        user.setResetToken(null);
        userRepository.save(user);
    }
}