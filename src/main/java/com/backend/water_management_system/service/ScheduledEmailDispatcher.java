package com.backend.water_management_system.service;

import com.backend.water_management_system.entity.MessageTemplate;
import com.backend.water_management_system.entity.ScheduledMessage;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.ScheduledMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ScheduledEmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledEmailDispatcher.class);

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final CustomerRepository customerRepository;
    private final MailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public ScheduledEmailDispatcher(ScheduledMessageRepository scheduledMessageRepository,
            CustomerRepository customerRepository,
            ObjectProvider<MailSender> mailSenderProvider) {
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.customerRepository = customerRepository;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    @Scheduled(fixedDelayString = "${app.messaging.scheduler-delay-ms:60000}")
    @Transactional
    public void sendDueScheduledEmails() {
        if (mailSender == null) {
            log.warn("MailSender bean is not available; skipping scheduled email dispatch");
            return;
        }

        List<ScheduledMessage> candidates = scheduledMessageRepository.findAllEmailSchedulable();
        if (candidates.isEmpty()) {
            return;
        }

        List<String> customerEmails = customerRepository.findAllCustomerEmails().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .distinct()
                .toList();

        if (customerEmails.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (ScheduledMessage message : candidates) {
            if (!isDue(message, now)) {
                continue;
            }

            String subject = buildSubject(message);
            String body = buildBody(message);
            int successCount = sendEmailToAll(customerEmails, subject, body);

            if (successCount > 0) {
                message.setLastEmailSentAt(now);
                if (isOneTime(message)) {
                    message.setOneTimeEmailSent(true);
                }
            }
        }
    }

    private int sendEmailToAll(List<String> customerEmails, String subject, String body) {
        int successCount = 0;

        for (String email : customerEmails) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                if (fromAddress != null && !fromAddress.isBlank()) {
                    mail.setFrom(fromAddress.trim());
                }
                mail.setTo(email);
                mail.setSubject(subject);
                mail.setText(body);
                mailSender.send(mail);
                successCount++;
            } catch (Exception ex) {
                log.warn("Failed to send scheduled email to {}: {}", email, ex.getMessage());
            }
        }

        return successCount;
    }

    private boolean isDue(ScheduledMessage message, LocalDateTime now) {
        if (message.getScheduleType() == null || message.getScheduleTime() == null) {
            return false;
        }

        if (isOneTime(message)) {
            LocalDate scheduledDate = message.getScheduleDate();
            if (scheduledDate == null || Boolean.TRUE.equals(message.getOneTimeEmailSent())) {
                return false;
            }

            return !now.toLocalDate().isBefore(scheduledDate)
                    && !now.toLocalTime().isBefore(message.getScheduleTime());
        }

        if (isRecurring(message)) {
            Integer dayOfMonth = message.getScheduleDayOfMonth();
            if (dayOfMonth == null || now.getDayOfMonth() != dayOfMonth) {
                return false;
            }

            LocalDate lastSentDate = message.getLastEmailSentAt() != null
                    ? message.getLastEmailSentAt().toLocalDate()
                    : null;

            return !now.toLocalTime().isBefore(message.getScheduleTime())
                    && (lastSentDate == null || !lastSentDate.equals(now.toLocalDate()));
        }

        return false;
    }

    private boolean isOneTime(ScheduledMessage message) {
        return "one-time".equalsIgnoreCase(message.getScheduleType());
    }

    private boolean isRecurring(ScheduledMessage message) {
        return "recurring".equalsIgnoreCase(message.getScheduleType());
    }

    private String buildSubject(ScheduledMessage message) {
        MessageTemplate emailTemplate = message.getEmailTemplate();
        if (emailTemplate != null && emailTemplate.getSubject() != null && !emailTemplate.getSubject().isBlank()) {
            return emailTemplate.getSubject();
        }

        if (message.getName() != null && !message.getName().isBlank()) {
            return message.getName();
        }

        return "Water Bill Message";
    }

    private String buildBody(ScheduledMessage message) {
        MessageTemplate emailTemplate = message.getEmailTemplate();
        if (emailTemplate == null) {
            return "";
        }

        if (emailTemplate.getContent() != null && !emailTemplate.getContent().isBlank()) {
            return emailTemplate.getContent();
        }

        if (emailTemplate.getSections() == null || emailTemplate.getSections().isEmpty()) {
            return "";
        }

        return emailTemplate.getSections().stream()
                .map(section -> section.getContent() == null ? "" : section.getContent())
                .filter(content -> !content.isBlank())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }
}
