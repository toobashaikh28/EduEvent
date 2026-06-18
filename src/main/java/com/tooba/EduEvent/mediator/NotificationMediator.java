package com.tooba.EduEvent.mediator;

public interface NotificationMediator {
    void notify(Object sender, String eventType, String targetUserId, String message);
}
