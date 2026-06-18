package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.SentNotification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SentNotificationRepository extends MongoRepository<SentNotification, String> {

    // An admin's most recent broadcasts, newest first — powers the Sent History panel
    List<SentNotification> findTop50BySenderIdOrderByCreatedAtDesc(String senderId);
}
