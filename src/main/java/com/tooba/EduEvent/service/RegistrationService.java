package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import java.util.List;

public interface RegistrationService {
    RegistrationResponse registerUserToEvent(Long userId, Long eventId);
    void cancelRegistration(Long userId, Long eventId);
    List<RegistrationResponse> getRegistrationsByEvent(Long eventId);
    List<RegistrationResponse> getRegistrationsByUser(Long userId);
}