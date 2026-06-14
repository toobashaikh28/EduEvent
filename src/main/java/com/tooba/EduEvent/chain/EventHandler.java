package com.tooba.EduEvent.chain;

import com.tooba.EduEvent.dto.request.EventRequest;
import com.tooba.EduEvent.entity.User;

public abstract class EventHandler {

    private EventHandler next;

    public EventHandler setNext(EventHandler next) {
        this.next = next;
        return next;
    }

    public abstract void handle(EventRequest request, User actor, String operation);

    protected void passToNext(EventRequest request, User actor, String operation) {
        if (next != null) {
            next.handle(request, actor, operation);
        }
    }
}
