package com.tooba.EduEvent.service.impl;

import com.tooba.EduEvent.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service // Singleton
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("EduEvent Engine <no-reply@eduevent.com>");

            mailSender.send(message);
            log.info("Real email successfully dispatched to target: {}", to);
        } catch (Exception e) {
            log.error("Failed to transmit email to {}. Stack Trace: ", to, e);
        }
    }
}