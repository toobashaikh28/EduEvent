package com.tooba.EduEvent.entity;

import com.tooba.EduEvent.pattern.UserInterface;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data                // Generates getters, setters, toString, equals, and hashCode
@Builder             // Enables the Builder pattern
@NoArgsConstructor   // Required by JPA
@AllArgsConstructor  // Required by @Builder
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
    private String role; // Logic handles 'USER', 'ADMIN', 'JUDGE'

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
    public boolean isNull() {
        return false; // A real user is never null
    }
}