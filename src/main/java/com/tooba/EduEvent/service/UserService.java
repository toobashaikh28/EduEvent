package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.UserRequest; // You might need to create this DTO
import com.tooba.EduEvent.dto.response.UserResponse;

public interface UserService {
    UserResponse getMyProfile(String email);
    UserResponse updateMyProfile(String email, UserRequest request);
}