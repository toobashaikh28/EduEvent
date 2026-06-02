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
    public void generate(User user, Event event, Integer rank) {
        Certificate certificate = Certificate.builder()
                .user(user)
                .event(event)
                .certUuid(UUID.randomUUID())
                .pdfUrl("https://example.com/certificates/" + UUID.randomUUID() + ".pdf")
                .issuedAt(LocalDateTime.now())
                .build();
        certificateRepository.save(certificate);
    }
}
