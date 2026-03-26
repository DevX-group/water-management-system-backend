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
            log.debug("No schedulable email messages found");
            return;
        }

        List<String> customerEmails = customerRepository.findAllCustomerEmails().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .distinct()
                .toList();

        if (customerEmails.isEmpty()) {
            log.warn("No customer emails found; skipping scheduled email dispatch");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int dueCount = 0;
        int totalSuccess = 0;

        for (ScheduledMessage message : candidates) {
            boolean due = isDue(message, now);
            log.info(
                    "Message due check: id={}, name='{}', type='{}', dayOfMonth={}, date={}, time={}, lastEmailSentAt={}, oneTimeEmailSent={}, due={}",
                    message.getId(),
                    message.getName(),
                    message.getScheduleType(),
                    message.getScheduleDayOfMonth(),
                    message.getScheduleDate(),
                    message.getScheduleTime(),
                    message.getLastEmailSentAt(),
                    message.getOneTimeEmailSent(),
                    due);

            if (!due) {
                continue;
            }

            dueCount++;

            String subject = buildSubject(message);
            String body = buildBody(message);
            int successCount = sendEmailToAll(customerEmails, subject, body);
            totalSuccess += successCount;

            if (successCount > 0) {
                message.setLastEmailSentAt(now);
                if (isOneTime(message)) {
                    message.setOneTimeEmailSent(true);
                }
            }
        }

        log.info("Scheduled email tick: candidates={}, due={}, recipients={}, successfulSends={}",
                candidates.size(), dueCount, customerEmails.size(), totalSuccess);
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
            if (dayOfMonth == null) {
                return false;
            }

            LocalDate lastSentDate = message.getLastEmailSentAt() != null
                    ? message.getLastEmailSentAt().toLocalDate()
                    : null;

            if (lastSentDate != null
                    && lastSentDate.getYear() == now.getYear()
                    && lastSentDate.getMonthValue() == now.getMonthValue()) {
                return false;
            }

            if (now.getDayOfMonth() < dayOfMonth) {
                return false;
            }

            if (now.getDayOfMonth() == dayOfMonth) {
                return !now.toLocalTime().isBefore(message.getScheduleTime());
            }

            return true;
        }

        return false;
    }

    private boolean isOneTime(ScheduledMessage message) {
        String scheduleType = normalizeScheduleType(message.getScheduleType());
        return "one-time".equals(scheduleType)
                || "one time".equals(scheduleType)
                || "onetime".equals(scheduleType)
                || "one_time".equals(scheduleType);
    }

    private boolean isRecurring(ScheduledMessage message) {
        return "recurring".equals(normalizeScheduleType(message.getScheduleType()));
    }

    private String normalizeScheduleType(String scheduleType) {
        if (scheduleType == null) {
            return "";
        }
        return scheduleType.trim().toLowerCase();
    }

    private String buildSubject(ScheduledMessage message) {
        MessageTemplate emailTemplate = message.getEmailTemplate();
        if (emailTemplate != null && emailTemplate.getSubject() != null && !emailTemplate.getSubject().isBlank()) {
            return emailTemplate.getSubject();
        }

        MessageTemplate smsTemplate = message.getSmsTemplate();
        if (smsTemplate != null && smsTemplate.getSubject() != null && !smsTemplate.getSubject().isBlank()) {
            return smsTemplate.getSubject();
        }

        if (message.getName() != null && !message.getName().isBlank()) {
            return message.getName();
        }

        return "Water Bill Message";
    }

    private String buildBody(ScheduledMessage message) {
        MessageTemplate emailTemplate = message.getEmailTemplate();
        String emailBody = buildBodyFromTemplate(emailTemplate);
        if (!emailBody.isBlank()) {
            return emailBody;
        }

        MessageTemplate smsTemplate = message.getSmsTemplate();
        return buildBodyFromTemplate(smsTemplate);
    }

    private String buildBodyFromTemplate(MessageTemplate template) {
        if (template == null) {
            return "";
        }

        if (template.getContent() != null && !template.getContent().isBlank()) {
            return template.getContent();
        }

        if (template.getSections() == null || template.getSections().isEmpty()) {
            return "";
        }

        return template.getSections().stream()
                .map(section -> section.getContent() == null ? "" : section.getContent())
                .filter(content -> !content.isBlank())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }
}
