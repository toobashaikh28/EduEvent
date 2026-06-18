package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.UserRequest;
import com.tooba.EduEvent.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getMyProfile(String email);
    UserResponse updateMyProfile(String email, UserRequest request);

    // ── Admin user management ──────────────────────────────
    List<UserResponse> getAllUsers();
    UserResponse adminCreateUser(String name, String email, String password, String role);
    UserResponse adminUpdateUser(String id, String name, String role);
    UserResponse setUserRole(String id, String role);
    UserResponse setUserActive(String id, boolean active);
    void deleteUser(String id);
}
