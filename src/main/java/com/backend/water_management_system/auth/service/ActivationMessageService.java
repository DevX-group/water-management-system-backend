package com.backend.water_management_system.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Activation email skipped: missing recipient email.");
            return;
        }

        String baseUrl = frontendUrl == null ? "" : frontendUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String activationLink = baseUrl + "/signup?token=" + token;

        SimpleMailMessage mail = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            mail.setFrom(fromEmail);
        }
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

        try {
            mailSender.send(mail);
        } catch (Exception ex) {
            log.error("Failed to send activation email to {}", toEmail, ex);
        }
    }

    // Future enhancement: public void sendActivationSms(String phoneNumber, String token) { ... }
}
