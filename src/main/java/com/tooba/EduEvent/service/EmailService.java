package com.tooba.EduEvent.service;

public interface EmailService {
    // Your existing method
    void sendEmail(String to, String subject, String body); 
    
    // The new method for certificates
    void sendCertificateEmail(String toEmail, String userName, String eventTitle, String pdfFilePath);
}