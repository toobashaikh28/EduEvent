package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.entity.Certificate;
import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.CertificateRepository;
import com.tooba.EduEvent.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;

    @Override
    public Certificate generate(User user, Event event) { // 1. Matched parameters and return type
        Certificate certificate = Certificate.builder()
                .user(user)
                .event(event)
                .certUuid(UUID.randomUUID())
                .pdfUrl("https://example.com/certificates/" + UUID.randomUUID() + ".pdf")
                .issuedAt(LocalDateTime.now())
                .build();
                
        // 2. Return the saved certificate to satisfy the 'Certificate' return type
        return certificateRepository.save(certificate); 
    }
}