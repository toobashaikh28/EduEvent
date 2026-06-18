package com.tooba.EduEvent.config;

import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds default accounts on startup so there's always an ADMIN, JUDGE and USER
 * to log in with — no manual registration / role selection needed.
 * Each account is only created if its email doesn't already exist.
 *
 *   ADMIN  →  admin@eduevent.com  / admin123
 *   JUDGE  →  judge@eduevent.com  / judge123
 *   USER   →  user@eduevent.com   / user123
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("Admin", "admin@eduevent.com", "admin123", "ADMIN");
        seed("Judge", "judge@eduevent.com", "judge123", "JUDGE");
        seed("User",  "user@eduevent.com",  "user123",  "USER");
    }

    private void seed(String name, String email, String rawPassword, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            return; // already seeded
        }
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .isActive(true)
                .build();
        userRepository.save(user);
        log.info("Seeded default {} account: {}", role, email);
    }
}
