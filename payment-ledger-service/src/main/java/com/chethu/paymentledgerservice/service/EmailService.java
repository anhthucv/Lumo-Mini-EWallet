package com.chethu.paymentledgerservice.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final String SUBJECT = "E-Wallet - Email Verification Code";
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText("""
                Your verification code is %s.

                This code expires in 5 minutes.
                If you did not request registration, you can ignore this email.
                """.formatted(code));
        mailSender.send(message);
    }
}
