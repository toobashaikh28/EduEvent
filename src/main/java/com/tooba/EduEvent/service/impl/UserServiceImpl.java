package com.tooba.EduEvent.service.impl;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.tooba.EduEvent.service.UserService;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.dto.response.UserResponse;
import com.tooba.EduEvent.dto.request.UserRequest; 

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return mapToResponse(user);
    }

    @Override
    public UserResponse updateMyProfile(String email, UserRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setName(req.getName());
        // Update other fields as needed
        
        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}