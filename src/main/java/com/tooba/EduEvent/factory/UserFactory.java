package com.tooba.EduEvent.factory;

import com.tooba.EduEvent.dto.request.RegisterRequest;
import com.tooba.EduEvent.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    // Helper to create a basic user shell
    private User.UserBuilder createBaseUser(RegisterRequest req) {
        return User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(req.getPassword()) // Note: Encoding will happen in Service layer later
                .isActive(true);
    }

    public User createUser(RegisterRequest req) {
        return createBaseUser(req).role("USER").build();
    }

    public User createAdmin(RegisterRequest req) {
        return createBaseUser(req).role("ADMIN").build();
    }

    public User createJudge(RegisterRequest req) {
        return createBaseUser(req).role("JUDGE").build();
    }

    // Day 4 Task: createByRole with switch
    public User createByRole(String role, RegisterRequest req) {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return createAdmin(req);
            case "JUDGE":
                return createJudge(req);
            case "USER":
            default:
                return createUser(req);
        }
    }
}