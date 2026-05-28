package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.service.security.JwtUtil;
import com.tooba.EduEvent.service.AuthService;
import com.tooba.EduEvent.service.EmailService;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.factory.UserFactory;
import com.tooba.EduEvent.pattern.NullUser;
import com.tooba.EduEvent.pattern.UserInterface;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.dto.request.LoginRequest;
import com.tooba.EduEvent.dto.request.RegisterRequest;
import com.tooba.EduEvent.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists.");
        }

        String role = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole()
                : "USER";

        User user = userFactory.createByRole(role, request);
        User savedUser = userRepository.save(user);

        log.info("New user registered: {} with role: {}", savedUser.getEmail(), savedUser.getRole());

        return UserResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    public String login(LoginRequest request) {
        UserInterface userInterface = userRepository.findByEmail(request.getEmail())
                .<UserInterface>map(u -> u)
                .orElse(new NullUser());

        if (userInterface.isNull()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = (User) userInterface;

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        log.info("User logged in: {}", user.getEmail());
        return jwtUtil.generateToken(user.getEmail());
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No account found with this email address."));

        String resetToken = UUID.randomUUID().toString();

        user.setResetToken(resetToken);
        // Fix: set expiry to 1 hour from now
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String resetLink = baseUrl + "/api/auth/reset-password?token=" + resetToken;
        String subject = "EduEvent — Password Reset Request";
        String body = "Hi " + user.getName() + ",\n\n"
                + "We received a request to reset your EduEvent password.\n\n"
                + "Click the link below to set a new password:\n"
                + resetLink + "\n\n"
                + "This link expires in 1 hour. If you did not request this, "
                + "please ignore this email.\n\n"
                + "Best,\nEduEvent Team";

        emailService.sendEmail(user.getEmail(), subject, body);
        log.info("Password reset email sent to: {}", email);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or expired password reset token."));

        // Fix: check expiry — reject tokens older than 1 hour
        if (user.getResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password reset token has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }
}
