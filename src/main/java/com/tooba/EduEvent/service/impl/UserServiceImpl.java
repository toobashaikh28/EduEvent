package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.dto.request.UserRequest;
import com.tooba.EduEvent.dto.response.UserResponse;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.UserRepository;
import com.tooba.EduEvent.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        return mapToResponse(user);
    }

    @Override
    public UserResponse updateMyProfile(String email, UserRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        user.setName(req.getName());

        // Fix: photoUrl was never saved — now it is
        if (req.getPhotoUrl() != null) {
            user.setPhoto(req.getPhotoUrl());
        }

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                // Fix: photo field on User maps to photoUrl on UserResponse
                .photoUrl(user.getPhoto())
                .build();
    }
}
