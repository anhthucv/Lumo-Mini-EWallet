package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {
    @Test
    void verificationEmailUsesConfiguredFromAddress() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, "lumo@example.test");

        service.sendVerificationCode("user@example.test", "123456");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertEquals("lumo@example.test", message.getValue().getFrom());
        assertEquals("user@example.test", message.getValue().getTo()[0]);
        assertEquals("E-Wallet - Email Verification Code", message.getValue().getSubject());
    }
}
