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

    // Runs every 10 minutes — transitions UPCOMING → LIVE when start_time has passed
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void updateUpcomingToLive() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> startingEvents = eventRepository.findByStartTimeBeforeAndStatus(now, "UPCOMING");
        for (Event event : startingEvents) {
            event.setStatus("LIVE");
            log.info("Event ID {} is now LIVE", event.getId());
        }
        eventRepository.saveAll(startingEvents);
    }

    // FIX #9: Changed from fixedDelay=20000 (20 seconds) to fixedRate=600000 (10 minutes).
    // fixedDelay counts from when the previous run FINISHES — at 20 s that runs ~180x/hour.
    // The task spec requires fixedRate (time between start of each run) at 10-minute intervals,
    // consistent with the UPCOMING→LIVE job above.
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void updateLiveToCompleted() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> endingEvents = eventRepository.findByEndTimeBeforeAndStatus(now, "LIVE");
        for (Event event : endingEvents) {
            event.setStatus("COMPLETED");
            log.info("Event ID {} is now COMPLETED", event.getId());
        }
        eventRepository.saveAll(endingEvents);
    }
}