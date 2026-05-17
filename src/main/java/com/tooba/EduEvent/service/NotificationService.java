package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {

    void send(Long userId, String title, String message);

    List<NotificationResponse> getNotificationsForUser(Long userId);

    NotificationResponse markAsRead(Long notificationId, Long userId);
}