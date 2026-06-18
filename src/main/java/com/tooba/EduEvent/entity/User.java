package com.tooba.EduEvent.entity;

import com.tooba.EduEvent.pattern.UserInterface;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserInterface {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String role; // 'USER', 'ADMIN', 'JUDGE'

    private String resetToken;

    // Fix: expiry timestamp so reset links expire after 1 hour
    private LocalDateTime resetTokenExpiry;

    private String photo;

    private String bio;

    private String city;

    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
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
