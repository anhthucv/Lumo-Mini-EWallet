package com.chethu.paymentledgerservice.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.domain.NotificationEventType;
import com.chethu.paymentledgerservice.event.FinancialNotificationEvent;

@Service
public class EmailService {
    private static final String SUBJECT = "E-Wallet - Email Verification Code";
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender, @Value("${mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText("""
                Your verification code is %s.

                This code expires in 5 minutes.
                If you did not request registration, you can ignore this email.
                """.formatted(code));
        mailSender.send(message);
    }

    public void sendFinancialNotification(String email, FinancialNotificationEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subjectFor(event.eventType()));
        message.setText(bodyFor(event));
        mailSender.send(message);
    }

    private String subjectFor(NotificationEventType eventType) {
        return switch (eventType) {
            case DEPOSIT_SUCCESS -> "E-Wallet - Deposit successful";
            case WITHDRAW_SUCCESS -> "E-Wallet - Withdrawal successful";
            case TRANSFER_SENT -> "E-Wallet - Transfer sent";
            case TRANSFER_RECEIVED -> "E-Wallet - Transfer received";
        };
    }

    private String bodyFor(FinancialNotificationEvent event) {
        String action = switch (event.eventType()) {
            case DEPOSIT_SUCCESS -> "Your deposit was successful.";
            case WITHDRAW_SUCCESS -> "Your withdrawal was successful.";
            case TRANSFER_SENT -> "Your transfer was sent successfully.";
            case TRANSFER_RECEIVED -> "You received a transfer successfully.";
        };
        String related = event.relatedAccountDisplay() == null
                ? ""
                : "\nRelated account: " + event.relatedAccountDisplay();
        return ("%s\n\nAmount: %s%s\nReference: %s\n")
                .formatted(action, event.amount(), related, event.transactionReference());
    }
}
