package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.request.UserRequest;
import com.tooba.EduEvent.dto.response.UserResponse;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> VALID_ROLES = Set.of("USER", "ADMIN", "JUDGE");

    @Override
    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToResponse(user);
    }

    @Override
    public UserResponse updateMyProfile(String email, UserRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setName(req.getName());
        if (req.getPhotoUrl() != null) {
            user.setPhoto(req.getPhotoUrl());
        }
        return mapToResponse(userRepository.save(user));
    }

    // ── Admin user management ──────────────────────────────

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse adminCreateUser(String name, String email, String rawPassword, String role) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String r = normalizeRole(role);
        String pwd = (rawPassword == null || rawPassword.isBlank()) ? "password123" : rawPassword;
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(pwd))
                .role(r)
                .isActive(true)
                .build();
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public UserResponse adminUpdateUser(String id, String name, String role) {
        User user = findUser(id);
        if (name != null && !name.isBlank()) user.setName(name);
        if (role != null && !role.isBlank()) user.setRole(normalizeRole(role));
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public UserResponse setUserRole(String id, String role) {
        User user = findUser(id);
        user.setRole(normalizeRole(role));
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public UserResponse setUserActive(String id, boolean active) {
        User user = findUser(id);
        user.setIsActive(active);
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }

    // ── helpers ────────────────────────────────────────────

    private User findUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeRole(String role) {
        if (role == null) return "USER";
        String r = role.trim().toUpperCase();
        if ("PARTICIPANT".equals(r)) r = "USER";   // UI label → internal role
        if (!VALID_ROLES.contains(r)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
        return r;
    }

    private UserResponse mapToResponse(User user) {
        long events = registrationRepository.findByUserId(user.getId()).size();
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .photoUrl(user.getPhoto())
                .createdAt(user.getCreatedAt())
                .active(user.getIsActive())
                .eventsCount(events)
                .build();
    }
}
