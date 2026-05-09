package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.factory.UserFactory;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.dto.request.LoginRequest;
import com.tooba.EduEvent.dto.request.RegisterRequest;
import com.tooba.EduEvent.dto.response.UserResponse;
import com.tooba.EduEvent.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // Constructor Injection for Singleton efficiency
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserResponse register(RegisterRequest request) {
        // 1. Use Factory to create the user entity based on role
        // We'll default to "USER" or pass it from the request if needed
        User user = userFactory.createByRole("USER", request.getName(), request.getEmail(), request.getPassword());
        
        // 2. Save to database
        User savedUser = userRepository.save(user);
        
        // 3. Map to DTO (Manual mapping for now to ensure no password leak)
        return UserResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    @Override
    public String login(LoginRequest request) {
        // Logic for login will include findByEmail and JWT generation in next step
        return "JWT_TOKEN_WILL_GO_HERE";
    }
}