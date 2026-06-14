package com.tooba.EduEvent.chain.handlers;

import com.tooba.EduEvent.chain.EventHandler;
import com.tooba.EduEvent.dto.request.EventRequest;
import com.tooba.EduEvent.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthorizationHandler extends EventHandler {

    @Override
    public void handle(EventRequest request, User actor, String operation) {
        if (!"ADMIN".equalsIgnoreCase(actor.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only administrators can " + operation.toLowerCase() + " events."
            );
        }
        System.out.println("[CHAIN] AuthorizationHandler: PASSED for " + actor.getEmail());
        passToNext(request, actor, operation);
    }
}
