package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.response.RegistrationResponse;
import java.util.List;

public interface WaitlistService {
    void promoteNext(Long eventId);
    List<RegistrationResponse> getWaitlistByEvent(Long eventId);
}