package com.tooba.EduEvent.chain.handlers;

import com.tooba.EduEvent.chain.EventHandler;
import com.tooba.EduEvent.dto.request.EventRequest;
import com.tooba.EduEvent.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EventValidationHandler extends EventHandler {

    @Override
    public void handle(EventRequest request, User actor, String operation) {
        if ("DELETE".equals(operation)) {
            passToNext(request, actor, operation);
            return;
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event title is required.");
        }

        if (request.getCapacity() != null && request.getCapacity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacity must be at least 1.");
        }

        if (request.getStartTime() != null && request.getEndTime() != null
                && request.getEndTime().isBefore(request.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time cannot be before start time.");
        }

        System.out.println("[CHAIN] ValidationHandler: PASSED for event '" + request.getTitle() + "'");
        passToNext(request, actor, operation);
    }
}
