package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Unread first, then by newest — used for GET /api/notifications
    List<Notification> findByUserIdOrderByIsReadAscCreatedAtDesc(Long userId);

    // Add this to get just the 5 most recent notifications
    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}