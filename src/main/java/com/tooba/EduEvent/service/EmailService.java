package com.tooba.EduEvent.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}