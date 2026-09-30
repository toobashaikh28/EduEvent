package com.tooba.EduEvent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight, public, DB-free endpoint. Point an uptime pinger (UptimeRobot /
 * cron-job.org) at /api/health every ~10 minutes so Render's free tier never
 * spins the service down (which causes the 30-60 s "cold start" delay).
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
