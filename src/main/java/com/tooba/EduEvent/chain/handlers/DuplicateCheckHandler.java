package com.tooba.EduEvent.chain.handlers;

import com.tooba.EduEvent.chain.EventHandler;
import com.tooba.EduEvent.dto.request.EventRequest;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class DuplicateCheckHandler extends EventHandler {

    private final EventRepository eventRepository;

    @Override
    public void handle(EventRequest request, User actor, String operation) {
        if ("CREATE".equals(operation)) {
            boolean exists = eventRepository.existsByTitleIgnoreCase(request.getTitle().trim());
            if (exists) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An event with the title '" + request.getTitle() + "' already exists."
                );
            }
            System.out.println("[CHAIN] DuplicateCheckHandler: No duplicate found.");
        }
        passToNext(request, actor, operation);
    }
}
