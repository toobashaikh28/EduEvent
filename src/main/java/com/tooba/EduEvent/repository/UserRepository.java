package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Custom query to find a user by email for Login/Security
    // Using Optional prevents NullPointerExceptions
    Optional<User> findByEmail(String email);
}