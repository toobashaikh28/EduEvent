package com.tooba.EduEvent.scheduler;

import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventScheduler {

    private final EventRepository eventRepository;

    // Task 3: Change UPCOMING to LIVE
    @Scheduled(fixedRate = 600000) // Runs every 10 minutes
    @Transactional
    public void updateUpcomingToLive() {
        LocalDateTime now = LocalDateTime.now();
        
        // Find events that should have started but are still marked UPCOMING
        List<Event> startingEvents = eventRepository.findByStartTimeBeforeAndStatus(now, "UPCOMING");
        
        for (Event event : startingEvents) {
            event.setStatus("LIVE");
            log.info("Event ID {} is now LIVE", event.getId());
        }
        eventRepository.saveAll(startingEvents);
    }

    // Task 4: Change LIVE to COMPLETED -- waits 20s after the previous run finishes
    @Scheduled(fixedDelay = 20000) 
    @Transactional
    public void updateLiveToCompleted() {
        LocalDateTime now = LocalDateTime.now();
        
        // Find events that have ended but are still marked LIVE
        List<Event> endingEvents = eventRepository.findByEndTimeBeforeAndStatus(now, "LIVE");
        
        for (Event event : endingEvents) {
            event.setStatus("COMPLETED");
            log.info("Event ID {} is now COMPLETED", event.getId());
        }
        eventRepository.saveAll(endingEvents);
    }
}