package com.tooba.EduEvent.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.tooba.EduEvent.entity.Certificate;
import com.tooba.EduEvent.entity.Event;
import com.tooba.EduEvent.entity.User;
import com.tooba.EduEvent.exception.CertificateGenerationException;
import com.tooba.EduEvent.repository.CertificateRepository;
import com.tooba.EduEvent.service.CertificateService;
import com.tooba.EduEvent.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final EmailService emailService; // 1. Injected EmailService

    @Override
    public Certificate generate(User user, Event event) {
        // 1. Null Object Pattern Check
        if (user == null || user.isNull()) {
            throw new CertificateGenerationException("Cannot generate certificate: User is null or invalid.");
        }
        if (event == null) {
            throw new CertificateGenerationException("Cannot generate certificate: Event is missing.");
        }

        UUID certId = UUID.randomUUID();
        String verificationUrl = "http://localhost:8080/api/certificates/verify/" + certId.toString();

        try {
            // 2. Ensure the directory exists (/certificates/{userId}/)
            String userDirPath = "certificates/" + user.getId() + "/";
            Path path = Paths.get(userDirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            String filePath = userDirPath + "cert_" + certId + ".pdf";

            // 3. Generate QR Code (200x200)
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(verificationUrl, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] qrBytes = pngOutputStream.toByteArray();

            // 4. Generate the PDF Document (Landscape mode)
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(new File(filePath)));

            document.open();

            // Define Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, BaseColor.BLACK);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 24, BaseColor.DARK_GRAY);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 18, BaseColor.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);

            // Add Content to PDF
            Paragraph title = new Paragraph("CERTIFICATE OF ACHIEVEMENT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(40);
            document.add(title);

            Paragraph awardedTo = new Paragraph("This is proudly presented to:", subtitleFont);
            awardedTo.setAlignment(Element.ALIGN_CENTER);
            document.add(awardedTo);

            Paragraph name = new Paragraph(user.getName(), titleFont);
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingAfter(30);
            document.add(name);

            Paragraph eventText = new Paragraph("For successful participation and outstanding performance in:", textFont);
            eventText.setAlignment(Element.ALIGN_CENTER);
            document.add(eventText);

            Paragraph eventName = new Paragraph(event.getTitle(), subtitleFont);
            eventName.setAlignment(Element.ALIGN_CENTER);
            eventName.setSpacingAfter(30);
            document.add(eventName);

            Paragraph dateText = new Paragraph("Awarded on: " + LocalDateTime.now().toLocalDate().toString(), textFont);
            dateText.setAlignment(Element.ALIGN_CENTER);
            dateText.setSpacingAfter(10);
            document.add(dateText);

            Paragraph certIdText = new Paragraph("Certificate ID: " + certId.toString(), smallFont);
            certIdText.setAlignment(Element.ALIGN_CENTER);
            certIdText.setSpacingAfter(20);
            document.add(certIdText);

            // Add embedded QR Code Image
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            document.add(qrImage);

            document.close();
            log.info("Certificate generated successfully for {} at {}", user.getName(), filePath);

            // 5. Save to Database
            Certificate certificate = Certificate.builder()
                    .user(user)
                    .event(event)
                    .certUuid(certId)
                    .pdfUrl(filePath) // Storing local path
                    .verifyUrl(verificationUrl) // 2. Added verifyUrl to the builder
                    .issuedAt(LocalDateTime.now())
                    .build();

            certificate = certificateRepository.save(certificate);

            // 6. Send the Email with PDF Attachment
            emailService.sendCertificateEmail(user.getEmail(), user.getName(), event.getTitle(), filePath);

            return certificate;

        } catch (Exception e) {
            log.error("Failed to generate PDF certificate", e);
            throw new CertificateGenerationException("Failed to generate PDF certificate", e);
        }
    }
}