package com.tooba.EduEvent.service;

import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.User;

public interface CertificateService {
    void generate(User user, Event event, Integer rank);
}
