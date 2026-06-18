package com.tooba.EduEvent.controller;

import com.tooba.EduEvent.dto.response.AdminCertificatesResponse;
import com.tooba.EduEvent.dto.response.CertificateResponse;
import com.tooba.EduEvent.entity.Certificate;
import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.RegistrationStatus;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.repository.CertificateRepository;
import com.tooba.EduEvent.repository.EventRepository;
import com.tooba.EduEvent.repository.RegistrationRepository;
import com.tooba.EduEvent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CertificateController {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

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

    // 2b. ADMIN — list every certificate + stat cards (all real data)
    @GetMapping("/admin/certificates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminCertificatesResponse> adminListCertificates() {
        List<Certificate> certs = certificateRepository.findAll();

        // resolve names/titles/types once
        Map<String, User> usersById = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<String, Event> eventsById = eventRepository.findAll().stream()
                .collect(Collectors.toMap(Event::getId, e -> e, (a, b) -> a));

        YearMonth thisMonth = YearMonth.now();
        long thisMonthCount = certs.stream()
                .filter(c -> c.getIssuedAt() != null && YearMonth.from(c.getIssuedAt()).equals(thisMonth))
                .count();

        // Pending: people REGISTERED for a COMPLETED event who don't have a certificate yet
        Set<String> issuedKeys = certs.stream()
                .map(c -> c.getUserId() + "|" + c.getEventId())
                .collect(Collectors.toCollection(HashSet::new));
        long pending = eventRepository.findByStatusIgnoreCase("COMPLETED").stream()
                .flatMap(ev -> registrationRepository
                        .findByEventIdAndStatus(ev.getId(), RegistrationStatus.REGISTERED).stream())
                .filter(r -> !issuedKeys.contains(r.getUserId() + "|" + r.getEventId()))
                .count();

        List<AdminCertificatesResponse.Row> rows = certs.stream()
                .sorted((a, b) -> {
                    if (a.getIssuedAt() == null) return 1;
                    if (b.getIssuedAt() == null) return -1;
                    return b.getIssuedAt().compareTo(a.getIssuedAt());
                })
                .map(c -> {
                    User u = usersById.get(c.getUserId());
                    Event ev = eventsById.get(c.getEventId());
                    return AdminCertificatesResponse.Row.builder()
                            .id(c.getId())
                            .certNumber(certNumber(c.getCertUuid()))
                            .recipient(u != null ? u.getName() : "Unknown")
                            .eventTitle(ev != null ? ev.getTitle() : "Unknown Event")
                            .eventType(ev != null && ev.getType() != null ? ev.getType().toLowerCase() : "other")
                            .issuedAt(c.getIssuedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(AdminCertificatesResponse.builder()
                .totalIssued(certs.size())
                .thisMonth(thisMonthCount)
                .pendingGeneration(pending)
                .certificates(rows)
                .build());
    }

    // 2c. ADMIN — revoke (delete) a certificate and its PDF file
    @DeleteMapping("/certificates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCertificate(@PathVariable String id) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        if (cert.getPdfUrl() != null && !cert.getPdfUrl().isBlank()) {
            try { Files.deleteIfExists(Paths.get(cert.getPdfUrl()).normalize()); }
            catch (Exception e) { log.warn("Could not delete PDF for cert {}: {}", id, e.getMessage()); }
        }
        certificateRepository.delete(cert);
        return ResponseEntity.noContent().build();
    }

    private static String certNumber(UUID uuid) {
        if (uuid == null) return "CERT-UNKNOWN";
        return "CERT-" + uuid.toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    // 3. DOWNLOAD PDF ENDPOINT
    @GetMapping("/certificates/{id}/download")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable String id, Authentication authentication) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Security check: Ensure only the owner (or an admin) can download it
        if (!cert.getUserId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
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
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Certificate_" + cert.getCertUuid() + ".pdf\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading file", e);
        }
    }

    // Helper method to convert Entity to DTO (resolves user/event by id)
    private CertificateResponse toResponse(Certificate cert) {
        String participantName = userRepository.findById(cert.getUserId())
                .map(User::getName).orElse("Unknown");
        String eventTitle = eventRepository.findById(cert.getEventId())
                .map(Event::getTitle).orElse("Unknown Event");

        return CertificateResponse.builder()
                .id(cert.getId())
                .participantName(participantName)
                .eventTitle(eventTitle)
                .certUuid(cert.getCertUuid())
                .verifyUrl(cert.getVerifyUrl())
                .issuedAt(cert.getIssuedAt())
                .build();
    }
}
