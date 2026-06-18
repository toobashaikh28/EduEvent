package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import java.util.List;

public interface WaitlistService {
    void promoteNext(String eventId);
    List<RegistrationResponse> getWaitlistByEvent(String eventId);
}