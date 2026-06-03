package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.response.CertificateResponse;
import com.tooba.EduEvent.entity.Certificate;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.CertificateRepository;
import com.tooba.EduEvent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;

    // 1. PUBLIC VERIFICATION ENDPOINT (No Auth Required)
    @GetMapping("/certificates/verify/{certUuid}")
    public ResponseEntity<CertificateResponse> verifyCertificate(@PathVariable UUID certUuid) {
        Certificate cert = certificateRepository.findByCertUuid(certUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Certificate ID"));

        return ResponseEntity.ok(toResponse(cert));
    }

    // 2. USER VIEWS ALL THEIR CERTIFICATES
    @GetMapping("/users/me/certificates")
    public ResponseEntity<List<CertificateResponse>> getMyCertificates(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        List<CertificateResponse> certs = certificateRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(certs);
    }

    // 3. DOWNLOAD PDF ENDPOINT
    @GetMapping("/certificates/{id}/download")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable Long id, Authentication authentication) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Security check: Ensure only the owner (or an admin) can download it
        if (!cert.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You can only download your own certificates.");
        }

        try {
            Path filePath = Paths.get(cert.getPdfUrl()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF file missing on server");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    // The "attachment" header forces the browser to download the file rather than trying to open it in a new tab
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Certificate_" + cert.getCertUuid() + ".pdf\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading file", e);
        }
    }

    // Helper method to convert Entity to DTO
    private CertificateResponse toResponse(Certificate cert) {
        return CertificateResponse.builder()
                .id(cert.getId())
                .participantName(cert.getUser().getName())
                .eventTitle(cert.getEvent().getTitle())
                .certUuid(cert.getCertUuid())
                .verifyUrl(cert.getVerifyUrl())
                .issuedAt(cert.getIssuedAt())
                .build();
    }
}