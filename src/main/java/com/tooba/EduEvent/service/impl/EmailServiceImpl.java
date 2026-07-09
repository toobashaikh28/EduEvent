package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

@Service // Singleton
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // Gmail's SMTP server rejects (or silently drops) mail whose "From" header
    // doesn't match the authenticated account, so we must send as the real
    // MAIL_USERNAME address rather than a made-up domain.
    @Value("${spring.mail.username}")
    private String fromAddress;

    // --- YOUR EXISTING METHOD (Plain Text) ---
    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("EduEvent <" + fromAddress + ">");

            mailSender.send(message);
            log.info("Real email successfully dispatched to target: {}", to);
        } catch (Exception e) {
            log.error("Failed to transmit email to {}. Stack Trace: ", to, e);
            // Surface the failure to callers (admin console) instead of
            // swallowing it and pretending the send succeeded.
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    // --- NEW METHOD (PDF Attachment) ---
    @Override
    @Async
    public void sendCertificateEmail(String toEmail, String userName, String eventTitle, String pdfFilePath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // The 'true' flag below enables attachments (multipart)
            MimeMessageHelper helper = new MimeMessageHelper(message, true); 

            helper.setTo(toEmail);
            helper.setSubject("🏆 Your Certificate for " + eventTitle);
            helper.setText("Congratulations " + userName + "!\n\nPlease find your official certificate attached.", false);
            helper.setFrom("EduEvent <" + fromAddress + ">");

            // Grab the PDF file from the local directory and attach it
            FileSystemResource file = new FileSystemResource(new File(pdfFilePath));
            helper.addAttachment("Certificate_" + eventTitle.replaceAll("\\s+", "_") + ".pdf", file);

            mailSender.send(message);
            log.info("Certificate email with PDF successfully dispatched to target: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to transmit certificate email to {}. Stack Trace: ", toEmail, e);
        }
    }
}