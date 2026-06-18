package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    // Custom query to find a user by email for Login/Security
    // Using Optional prevents NullPointerExceptions
    Optional<User> findByResetToken(String resetToken);
    Optional<User> findByEmail(String email);
}
