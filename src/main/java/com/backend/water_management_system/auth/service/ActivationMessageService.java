package com.backend.water_management_system.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class ActivationMessageService {

    private final MailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.activation-token-expiry-hours:72}")
    private int activationTokenExpiryHours;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    public ActivationMessageService(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendActivationEmail(String toEmail, String token) {
        String activationLink = frontendUrl + "/activate?token=" + token;

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromEmail);
        mail.setTo(toEmail);
        mail.setSubject("Activate your HydroPay account");
        mail.setText(
                "Welcome to HydroPay!\n\n" +
                "Please click the link below to activate your account and set your password.\n" +
                "This link is valid for " + activationTokenExpiryHours + " hours.\n\n" +
                activationLink + "\n\n" +
                "If you did not expect this email, please ignore it.\n\n" +
                "HydroPay Water Management System"
        );

        mailSender.send(mail);
    }

    // Future enhancement: public void sendActivationSms(String phoneNumber, String token) { ... }
}
