package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {

    void send(String userId, String title, String message);

    List<NotificationResponse> getNotificationsForUser(String userId);

    NotificationResponse markAsRead(String notificationId, String userId);
}