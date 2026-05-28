package com.tooba.EduEvent.entity;

import com.tooba.EduEvent.pattern.UserInterface;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String role; // 'USER', 'ADMIN', 'JUDGE'

    @Column(name = "reset_token")
    private String resetToken;

    // Fix: expiry timestamp so reset links expire after 1 hour
    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(length = 500)
    private String photo;

    @Column(length = 1000)
    private String bio;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public String getRole() {
        return this.role;
    }

    @Override
    public boolean isNull() {
        return false;
    }
}
