package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.LoginRequest;
import com.tooba.EduEvent.dto.request.RegisterRequest;
import com.tooba.EduEvent.dto.response.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    String login(LoginRequest request);

    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}