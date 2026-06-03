package com.tooba.EduEvent.service;

import com.tooba.EduEvent.entity.Certificate;
import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.User;

public interface CertificateService {
    /**
     * Generates and persists a Certificate for the given user and event.
     * Idempotent — calling twice for the same user+event returns the existing one.
     */
    Certificate generate(User user, Event event);
}
