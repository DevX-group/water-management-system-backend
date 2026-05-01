package com.backend.water_management_system.service;

import com.backend.water_management_system.entity.MessageTemplate;
import com.backend.water_management_system.dto.SMSGatewayResponseDTO;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.ScheduledMessage;
import com.backend.water_management_system.entity.SentMessage;
import com.backend.water_management_system.entity.TemplateSection;
import com.backend.water_management_system.repository.BillRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.ScheduledMessageRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class ScheduledMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledMessageDispatcher.class);

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final SentMessageService sentMessageService;
    private final MailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${text-lk.api-endpoint:}")
    private String textLkApiEndpoint;

    @Value("${text-lk.api-token:}")
    private String textLkApiToken;

    @Value("${text-lk.sender-id:}")
    private String textLkSenderId;

    private WebClient webClient;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.%-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public ScheduledMessageDispatcher(ScheduledMessageRepository scheduledMessageRepository,
            CustomerRepository customerRepository,
            BillRepository billRepository,
            SentMessageService sentMessageService,
            ObjectProvider<MailSender> mailSenderProvider) {
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.customerRepository = customerRepository;
        this.billRepository = billRepository;
        this.sentMessageService = sentMessageService;
        this.mailSender = mailSenderProvider.getIfAvailable();
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
        int totalSuccess = 0;

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

            int successCount = dispatchMessageToAll(customers, message);

            totalSuccess += successCount;

            // if at least one message (email or sms) is successfully sent, update lastMessageSentAt or oneTimeMessageSent properties
            if (successCount > 0) {
                message.setLastMessageSentAt(now);
                if (isOneTime(message)) {
                    message.setOneTimeMessageSent(true);
                }
            }
        }

        // after all the due messages are processed, log the summary of this tick
        log.info("Scheduled messaging tick: candidates={}, due={}, recipients={}, successfulSends={}",
                candidates.size(), dueCount, customers.size(), totalSuccess);
    }

    //Dispatches a due message as a SMS and/or Email and returns number of successful sends
    private int dispatchMessageToAll(List<Customer> customers, ScheduledMessage message) {

        String subjectTemplate = buildSubject(message);
        String emailBodyTemplate = buildBodyFromTemplate(message.getEmailTemplate());
        String smsBodyTemplate = buildBodyFromTemplate(message.getSmsTemplate());

        String fromAddressForMail = resolveFromAddress();

        String channels = message.getChannels() != null ? message.getChannels().toLowerCase() : "";
        boolean shouldSendSMS = channels.contains("sms");
        boolean shouldSendEmail = channels.contains("email");

        int successCount = 0;

        for (Customer customer : customers) {
            // prepare current bill for placeholders
            Bill currentBill = billRepository.findTopByCustomerOrderByBillDateDesc(customer).orElse(null);

            //SMS attempt
            if(shouldSendSMS){
                String toPhone = customer.getMobileNumber() != null ? customer.getMobileNumber().trim() : "";
                if (!toPhone.isEmpty()) {
                    String smsTemplateToUse = resolveTemplateBody(smsBodyTemplate, emailBodyTemplate);
                    
                    boolean smsOk = dispatchSMS(customer, toPhone, smsTemplateToUse, currentBill);

                    if(smsOk){
                        successCount++;
                    }
                }
            }

            //Email attempt (only if MailSender exists and customer has an email)
            if (shouldSendEmail && mailSender != null) {
                String toEmail = customer.getEmail() != null ? customer.getEmail().trim() : "";
                if (isValidEmail(toEmail)) {
                    String emailTemplateToUse = resolveTemplateBody(emailBodyTemplate, smsBodyTemplate);
                    
                    boolean emailOk = dispatchEmail(customer, toEmail, fromAddressForMail, subjectTemplate, emailTemplateToUse, currentBill);
                    
                    if(emailOk){
                        successCount++;
                    }
                        
                }
            }
        }

        return successCount;
    }

    //dispatches a due message to a single customer as a SMS
    private boolean dispatchSMS(Customer customer, String toPhone, String smsTemplateToUse, Bill currentBill){
        String smsBody = replacePlaceholders(smsTemplateToUse, customer, currentBill);
        
        boolean smsOk = sendSms(toPhone, smsBody);
        
        return smsOk;
    }

    //dispatches a due message to a single customer as an email
    private boolean dispatchEmail(Customer customer, 
                                String toEmail,
                                String fromAddressForMail, 
                                String subjectTemplate, 
                                String emailTemplateToUse,
                                Bill currentBill){

        String subject = replacePlaceholders(subjectTemplate, customer, currentBill);
        String body = replacePlaceholders(emailTemplateToUse, customer, currentBill);
        
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

    // Sends SMS using Text.lk gateway. Returns true if the gateway returned a successful response.
    private boolean sendSms(String to, String message) {
        if (textLkApiEndpoint == null || textLkApiEndpoint.isBlank()
                || textLkApiToken == null || textLkApiToken.isBlank()) {
            log.warn("Text.lk SMS gateway not configured; skipping SMS to {}", to);
            return false;
        }

        if (webClient == null) {
            webClient = WebClient.builder().baseUrl(textLkApiEndpoint).build();
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("recipient", to);
            if (textLkSenderId != null && !textLkSenderId.isBlank()) {
                payload.put("sender_id", textLkSenderId);
            }
            payload.put("type", "plain");
            payload.put("message", message == null ? "" : message);

            SMSGatewayResponseDTO response = webClient.post()
                    .uri("")
                    .header("Authorization", "Bearer " + textLkApiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchangeToMono(resp -> 
                        resp.bodyToMono(SMSGatewayResponseDTO.class)
                            .defaultIfEmpty(new SMSGatewayResponseDTO())
                    )
                    .block();
                    
            if (response == null) {
                log.warn("Text.lk SMS request failed for {}: empty response", to);
                return false;
            }

            if (response.getStatus() != null && "success".equalsIgnoreCase(response.getStatus())) {
                return true;
            }

            log.warn("Text.lk SMS request failed for {} with status {} and message {}", to, response.getStatus(), response.getMessage());
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

    //if the template body is not available for the relevant channel, replace it with the template body of the other channel
    private String resolveTemplateBody(String primaryTemplate, String fallbackTemplate){
        return (primaryTemplate != null && !primaryTemplate.isBlank() ? primaryTemplate : fallbackTemplate);
    }

    // replaces placeholders in the template with actual values from the relevant customer and their current bill, if available.
    private String replacePlaceholders(String template, Customer customer, Bill currentBill) {
        if (template == null || template.isBlank()) {
            return "";
        }

        Map<String, String> values = new HashMap<>();
        values.put("customer_name", safe(customer != null ? customer.getAccountHolderName() : null));
        values.put("customer_number", safe(customer != null ? customer.getSubscriptionNumber() : null));
        values.put("outstanding_balance", formatNumber(customer != null ? customer.getOutstandingBalance() : null));
        values.put("outstanding balance", formatNumber(customer != null ? customer.getOutstandingBalance() : null));

        values.put("billing_period", safe(currentBill != null ? currentBill.getBillingPeriod() : null));
        values.put("bill_date", formatDate(currentBill != null ? currentBill.getBillDate() : null));
        values.put("base_charge", formatNumber(currentBill != null ? currentBill.getBaseCharge() : null));
        values.put("usage_units", formatInt(currentBill != null ? currentBill.getUsageUnits() : null));
        values.put("usage_charge", formatNumber(currentBill != null ? currentBill.getUsageCharge() : null));
        values.put("tax_amount", formatNumber(currentBill != null ? currentBill.getTaxAmount() : null));
        values.put("monthly_fee", formatNumber(currentBill != null ? currentBill.getTotalAmount() : null));
        values.put("total_balance", formatNumber(currentBill != null ? currentBill.getBalanceDue() : null));
        values.put("due_date", formatDate(currentBill != null ? currentBill.getDueDate() : null));

        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String formatNumber(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String formatInt(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    // returns whether the actual date and time the message should be sent is passed
    private boolean isDue(ScheduledMessage message, LocalDateTime now) {
        if (message.getScheduleType() == null || message.getScheduleTime() == null) {
            return false;
        }

        // if it is one-time, return whether the current date and time is after the scheduled date and time
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

            // if it is at least sent once and the last sent date is within this month this year, return false
            if (lastSentDate != null
                    && lastSentDate.getYear() == now.getYear()
                    && lastSentDate.getMonthValue() == now.getMonthValue()) {
                return false;
            }

            // if the current day of month is before the scheduled day of month, return false
            if (now.getDayOfMonth() < dayOfMonth) {
                return false;
            }

            // if the current day of month is the scheduled day of month, return whether the current time not is before the scheduled time
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

        //if it is a custom template and the content is not empty, return the content
        if (template.getContent() != null && !template.getContent().isBlank()) {
            return template.getContent();
        }

        /*if it is a custom template but there is nothing in content, 
        or if it is not a custom template but there is nothing in the template sections, 
        return an empty string */
        if (template.getSections() == null || template.getSections().isEmpty()) {
            return "";
        }

        //if it is not a custom template and there is content available in the template sections, return the collected content
        return template.getSections().stream()
                .map(section -> section.getContent() == null ? "" : section.getContent())
                .filter(content -> !content.isBlank())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    private SentMessage toSentMessage(ScheduledMessage scheduledMessage,
            LocalDateTime now,
            double emailSuccessRate,
            int totalSent,
            int totalFailed,
            int totalDelivered) {
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
        sentMessage.setSmsSuccessRate(0.0);
        sentMessage.setTotalEmailsSent(totalSent);
        sentMessage.setTotalEmailsFailed(totalFailed);
        sentMessage.setTotalEmailsDelivered(totalDelivered);
        sentMessage.setTotalSMSsSent(0);
        sentMessage.setTotalSMSsFailed(0);
        sentMessage.setTotalSMSsDelivered(0);
        return sentMessage;
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