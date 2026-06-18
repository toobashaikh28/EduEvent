package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import java.util.List;

public interface RegistrationService {
    RegistrationResponse registerUserToEvent(String userId, String eventId);
    void cancelRegistration(String userId, String eventId);
    List<RegistrationResponse> getRegistrationsByEvent(String eventId);
    List<RegistrationResponse> getRegistrationsByUser(String userId);
}