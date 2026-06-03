package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.response.DashboardResponse;
import com.tooba.EduEvent.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getMyDashboard(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getDashboard(authentication.getName()));
    }
}