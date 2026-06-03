package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(String email);
}