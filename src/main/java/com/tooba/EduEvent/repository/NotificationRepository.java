package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Unread first, then by newest — used for GET /api/notifications
    List<Notification> findByUserIdOrderByIsReadAscCreatedAtDesc(String userId);

    // The 5 most recent notifications
    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(String userId);
}
