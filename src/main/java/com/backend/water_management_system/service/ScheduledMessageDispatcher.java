package com.backend.water_management_system.service;

import com.backend.water_management_system.entity.MessageTemplate;
import com.backend.water_management_system.dto.SMSGatewayRequestDTO;
import com.backend.water_management_system.dto.SMSGatewayResponseDTO;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.ScheduledMessage;
import com.backend.water_management_system.entity.SentMessage;
import com.backend.water_management_system.entity.SentMessageFailure;
import com.backend.water_management_system.entity.TemplateSection;
import com.backend.water_management_system.repository.BillRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.ScheduledMessageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ScheduledMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledMessageDispatcher.class);

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final SentMessageService sentMessageService;
    private final MessagePlaceholderService messagePlaceholderService;
    private final ObjectProvider<MailSender> mailSenderProvider;
    private MailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${text-lk.api-endpoint:}")
    private String textLkApiEndpoint;

    @Value("${text-lk.api-token:}")
    private String textLkApiToken;

    @Value("${text-lk.sender-id:}")
    private String textLkSenderId;

    private final String smsType = "plain";

    private WebClient webClient;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.%-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @PostConstruct
    private void initMailSender() {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.webClient = WebClient.create();
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @Scheduled(fixedDelayString = "${app.messaging.scheduler-delay-ms:60000}")
    @Transactional
    public void sendDueScheduledMessages() {
        boolean canSendEmail = mailSender != null;
        boolean canSendSms = textLkApiEndpoint != null && !textLkApiEndpoint.isBlank()
                && textLkApiToken != null && !textLkApiToken.isBlank();

        if (!canSendEmail && !canSendSms) {
            log.warn("No MailSender or SMS gateway configured; skipping scheduled message dispatch");
            return;
        }

        // make a list of all schedulable messages
        List<ScheduledMessage> candidates = scheduledMessageRepository.findAllEmailSchedulableWithLock();
        if (candidates.isEmpty()) {
            log.debug("No schedulable messages found");
            return;
        }

        // make a list of ALL customers (every customer is a candidate for SMS)
        List<Customer> customers = customerRepository.findAll().stream()
                .filter(Objects::nonNull)
                .toList();

        if (customers.isEmpty()) {
            log.warn("No customers found; skipping scheduled message dispatch");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int dueCount = 0;

        for (ScheduledMessage message : candidates) {
            boolean due = isDue(message, now);
            log.info(
                    "Message due check: id={}, name='{}', type='{}', dayOfMonth={}, date={}, time={}, lastMessageSentAt={}, oneTimeMessageSent={}, due={}",
                    message.getId(),
                    message.getName(),
                    message.getScheduleType(),
                    message.getScheduleDayOfMonth(),
                    message.getScheduleDate(),
                    message.getScheduleTime(),
                    message.getLastMessageSentAt(),
                    message.getOneTimeMessageSent(),
                    due);

            if (!due) {
                continue;
            }

            dueCount++;

            DispatchCounts counts = dispatchMessageToAll(customers, message, canSendSms, canSendEmail);

            int totalEmailsFailed = Math.max(counts.totalEmails - counts.emailSuccessCount, 0);
            int totalSmsFailed = Math.max(counts.totalSms - counts.smsSuccessCount, 0);

            double emailSuccessRate = counts.totalEmails == 0
                    ? 0.0
                    : (counts.emailSuccessCount * 100.0) / counts.totalEmails;
            double smsSuccessRate = counts.totalSms == 0
                    ? 0.0
                    : (counts.smsSuccessCount * 100.0) / counts.totalSms;

            // if at least one message (email or sms) is successfully sent, save and update
            // send state
            if (counts.emailSuccessCount > 0 || counts.smsSuccessCount > 0) {
                sentMessageService.save(toSentMessage(
                        message,
                        now,
                        emailSuccessRate,
                        smsSuccessRate,
                        counts.totalEmails,
                        totalEmailsFailed,
                        counts.emailSuccessCount,
                        counts.totalSms,
                        totalSmsFailed,
                        counts.smsSuccessCount,
                        counts.failedRecipients));

                message.setLastMessageSentAt(now);
                if (isOneTime(message)) {
                    message.setOneTimeMessageSent(true);
                }
            }
        }

        // after all the due messages are processed, log the summary of this tick
        log.info("Scheduled messaging tick: candidates={}, due={}, recipients={}",
                candidates.size(), dueCount, customers.size());
    }

    // Dispatches a due message as a SMS and/or Email and returns per-channel counts
    private DispatchCounts dispatchMessageToAll(List<Customer> customers, ScheduledMessage message, boolean canSendSMS,
            boolean canSendEmail) {

        String subjectTemplate = buildSubject(message);
        String emailBodyTemplate = buildBodyFromTemplate(message.getEmailTemplate());
        String smsBodyTemplate = buildBodyFromTemplate(message.getSmsTemplate());

        String fromAddressForMail = resolveFromAddress();

        String channels = message.getChannels() != null ? message.getChannels().toLowerCase() : "";
        boolean shouldSendSMS = channels.contains("sms");
        boolean shouldSendEmail = channels.contains("email");

        if (shouldSendSMS && !canSendSMS) {
            log.warn("Message {} should be sent as a SMS but SMS gateway is not configured. Skipping SMS.",
                    message.getName());
        }

        if (shouldSendEmail && !canSendEmail) {
            log.warn("Message {} should be sent as an Email but MailSender is not configured. Skipping Email.",
                    message.getName());
        }

        DispatchCounts counts = new DispatchCounts();

        for (Customer customer : customers) {
            // prepare current bill for placeholders
            Bill currentBill = billRepository.findTopByCustomerOrderByBillDateDesc(customer).orElse(null);

            // SMS attempt
            boolean smsAttempted = false;
            boolean smsFailed = false;
            boolean emailAttempted = false;
            boolean emailFailed = false;

            // SMS attempt
            if (shouldSendSMS && canSendSMS) {
                String toPhone = customer.getMobileNumber() != null ? customer.getMobileNumber().trim() : "";
                if (!toPhone.isEmpty()) {
                    smsAttempted = true;
                    counts.totalSms++;
                    String smsTemplateToUse = resolveTemplateBody(smsBodyTemplate, emailBodyTemplate);

                    boolean smsOk = dispatchSMS(customer, toPhone, smsTemplateToUse, currentBill);

                    if (smsOk) {
                        counts.smsSuccessCount++;
                    } else {
                        smsFailed = true;
                    }
                }
            }

            // Email attempt (only if customer has an email)
            if (shouldSendEmail && canSendEmail) {
                String toEmail = customer.getEmail() != null ? customer.getEmail().trim() : "";
                if (isValidEmail(toEmail)) {
                    emailAttempted = true;
                    counts.totalEmails++;
                    String emailTemplateToUse = resolveTemplateBody(emailBodyTemplate, smsBodyTemplate);

                    boolean emailOk = dispatchEmail(customer, toEmail, fromAddressForMail, subjectTemplate,
                            emailTemplateToUse, currentBill);

                    if (emailOk) {
                        counts.emailSuccessCount++;
                    } else {
                        emailFailed = true;
                    }

                }
            }

            if ((smsAttempted && smsFailed) || (emailAttempted && emailFailed)) {
                SentMessageFailure failure = new SentMessageFailure();
                failure.setCustomer(customer);
                failure.setSmsFailed(smsAttempted && smsFailed);
                failure.setEmailFailed(emailAttempted && emailFailed);

                counts.failedRecipients.add(failure);
            }
        }

        return counts;
    }

    // dispatches a due message to a single customer as a SMS
    private boolean dispatchSMS(Customer customer, String toPhone, String smsTemplateToUse, Bill currentBill) {
        String smsBody = messagePlaceholderService.replacePlaceholders(smsTemplateToUse, customer, currentBill);

        boolean smsOk = sendSms(toPhone, smsBody);

        return smsOk;
    }

    // dispatches a due message to a single customer as an email
    private boolean dispatchEmail(Customer customer,
            String toEmail,
            String fromAddressForMail,
            String subjectTemplate,
            String emailTemplateToUse,
            Bill currentBill) {

        String subject = messagePlaceholderService.replacePlaceholders(subjectTemplate, customer, currentBill);
        String body = messagePlaceholderService.replacePlaceholders(emailTemplateToUse, customer, currentBill);

        try {
            SimpleMailMessage mail = new SimpleMailMessage();

            if (!fromAddressForMail.isBlank()) {
                mail.setFrom(fromAddressForMail);
            }

            mail.setTo(toEmail);
            mail.setSubject(subject);
            mail.setText(body);

            mailSender.send(mail);

            return true;
        } catch (Exception ex) {
            log.warn("Failed to send scheduled email to {}: {}", toEmail, ex.getMessage());
            return false;
        }
    }

    // Sends SMS using Text.lk gateway. Returns true if the gateway returned a
    // successful response.
    private boolean sendSms(String to, String message) {

        try {
            SMSGatewayRequestDTO payload = new SMSGatewayRequestDTO();
            payload.setRecipient(to);
            if (textLkSenderId != null && !textLkSenderId.isBlank()) {
                payload.setSender_id(textLkSenderId);
            }
            payload.setType(smsType);
            payload.setMessage(message == null ? "" : message);

            SMSGatewayResponseDTO response = webClient.post()
                    .uri(textLkApiEndpoint)
                    .header("Authorization", "Bearer " + textLkApiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchangeToMono(resp -> resp.bodyToMono(SMSGatewayResponseDTO.class)
                            .defaultIfEmpty(new SMSGatewayResponseDTO()))
                    .block();

            if (response == null) {
                log.warn("Text.lk SMS request failed for {}: empty response", to);
                return false;
            }

            if (response.getStatus() != null && "success".equalsIgnoreCase(response.getStatus())) {
                return true;
            }

            log.warn("Text.lk SMS request failed for {} with status {} and message {}", to, response.getStatus(),
                    response.getMessage());
            return false;
        } catch (Exception ex) {
            log.warn("Failed to send scheduled SMS to {}: {}", to, ex.getMessage());
            return false;
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private String resolveFromAddress() {
        if (fromAddress != null && !fromAddress.isBlank())
            return getFromAddress().trim();

        return "";
    }

    // if the template body is not available for the relevant channel, replace it
    // with the template body of the other channel
    private String resolveTemplateBody(String primaryTemplate, String fallbackTemplate) {
        return (primaryTemplate != null && !primaryTemplate.isBlank() ? primaryTemplate : fallbackTemplate);
    }

    // returns whether the actual date and time the message should be sent is passed
    private boolean isDue(ScheduledMessage message, LocalDateTime now) {
        if (message.getScheduleType() == null || message.getScheduleTime() == null) {
            return false;
        }

        // if it is one-time, return whether the current date and time is after the
        // scheduled date and time
        if (isOneTime(message)) {
            LocalDate scheduledDate = message.getScheduleDate();
            if (scheduledDate == null) {
                return false;
            }

            return !now.toLocalDate().isBefore(scheduledDate)
                    && !now.toLocalTime().isBefore(message.getScheduleTime());
        }

        // if it is recurring,
        if (isRecurring(message)) {
            Integer dayOfMonth = message.getScheduleDayOfMonth();

            // if scheduled day of month is not set, return false
            if (dayOfMonth == null) {
                return false;
            }

            LocalDate lastSentDate = message.getLastMessageSentAt() != null
                    ? message.getLastMessageSentAt().toLocalDate()
                    : null;

            // if it is at least sent once and the last sent date is within this month this
            // year, return false
            if (lastSentDate != null
                    && lastSentDate.getYear() == now.getYear()
                    && lastSentDate.getMonthValue() == now.getMonthValue()) {
                return false;
            }

            // if the current day of month is before the scheduled day of month, return
            // false
            if (now.getDayOfMonth() < dayOfMonth) {
                return false;
            }

            // if the current day of month is the scheduled day of month, return whether the
            // current time not is before the scheduled time
            if (now.getDayOfMonth() == dayOfMonth) {
                return !now.toLocalTime().isBefore(message.getScheduleTime());
            }

            // if the current date is after the scheduled date, return true
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

        // if there is no subject entered, return the message name as the subject
        if (message.getName() != null && !message.getName().isBlank()) {
            return message.getName();
        }

        return "Pradeshiya Sabha Water Bill";
    }

    private String buildBodyFromTemplate(MessageTemplate template) {
        if (template == null) {
            return "";
        }

        // if it is a custom template and the content is not empty, return the content
        if (template.getContent() != null && !template.getContent().isBlank()) {
            return template.getContent();
        }

        /*
         * if it is a custom template but there is nothing in content,
         * or if it is not a custom template but there is nothing in the template
         * sections,
         * return an empty string
         */
        if (template.getSections() == null || template.getSections().isEmpty()) {
            return "";
        }

        // if it is not a custom template and there is content available in the template
        // sections, return the collected content
        return template.getSections().stream()
                .map(section -> section.getContent() == null ? "" : section.getContent())
                .filter(content -> !content.isBlank())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    private SentMessage toSentMessage(ScheduledMessage scheduledMessage,
            LocalDateTime now,
            double emailSuccessRate,
            double smsSuccessRate,
            int totalEmailsSent,
            int totalEmailsFailed,
            int totalEmailsDelivered,
            int totalSmsSent,
            int totalSmsFailed,
            int totalSmsDelivered,
            List<SentMessageFailure> failedRecipients) {
        SentMessage sentMessage = new SentMessage();
        sentMessage.setSourceScheduledMessageId(scheduledMessage.getId());
        sentMessage.setName(scheduledMessage.getName());
        sentMessage.setChannels(scheduledMessage.getChannels());
        sentMessage.setRecipients(scheduledMessage.getRecipients());
        sentMessage.setDefault(scheduledMessage.isDefault());
        sentMessage.setSmsTemplate(copyTemplate(scheduledMessage.getSmsTemplate()));
        sentMessage.setEmailTemplate(copyTemplate(scheduledMessage.getEmailTemplate()));
        sentMessage.setSentDate(now.toLocalDate());
        sentMessage.setSentTime(now.toLocalTime());
        sentMessage.setEmailSuccessRate(emailSuccessRate);
        sentMessage.setSmsSuccessRate(smsSuccessRate);
        sentMessage.setTotalEmailsSent(totalEmailsSent);
        sentMessage.setTotalEmailsFailed(totalEmailsFailed);
        sentMessage.setTotalEmailsDelivered(totalEmailsDelivered);
        sentMessage.setTotalSMSsSent(totalSmsSent);
        sentMessage.setTotalSMSsFailed(totalSmsFailed);
        sentMessage.setTotalSMSsDelivered(totalSmsDelivered);
        if (failedRecipients != null && !failedRecipients.isEmpty()) {
            failedRecipients.forEach(failure -> failure.setSentMessage(sentMessage));
            sentMessage.setFailedRecipients(failedRecipients);
        }
        return sentMessage;
    }

    private static class DispatchCounts {
        private int totalEmails;
        private int emailSuccessCount;
        private int totalSms;
        private int smsSuccessCount;
        private List<SentMessageFailure> failedRecipients = new ArrayList<>();
    }

    private MessageTemplate copyTemplate(MessageTemplate source) {
        if (source == null) {
            return null;
        }

        MessageTemplate copy = new MessageTemplate();
        copy.setCustom(source.isCustom());
        copy.setContent(source.getContent());
        copy.setSubject(source.getSubject());

        if (source.getSections() != null) {
            List<TemplateSection> sections = source.getSections().stream().map(section -> {
                TemplateSection clone = new TemplateSection();
                clone.setSectionKey(section.getSectionKey());
                clone.setName(section.getName());
                clone.setContent(section.getContent());
                clone.setSectionOrder(section.getSectionOrder());
                clone.setMessageTemplate(copy);
                return clone;
            }).toList();
            copy.setSections(sections);
        }

        return copy;
    }
}