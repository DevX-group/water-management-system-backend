package com.backend.water_management_system.service;

import com.backend.water_management_system.entity.MessageTemplate;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ScheduledEmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledEmailDispatcher.class);

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final SentMessageService sentMessageService;
    private final MailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public ScheduledEmailDispatcher(ScheduledMessageRepository scheduledMessageRepository,
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
    public void sendDueScheduledEmails() {
        if (mailSender == null) {
            log.warn("MailSender bean is not available; skipping scheduled email dispatch");
            return;
        }

        List<ScheduledMessage> candidates = scheduledMessageRepository.findAllEmailSchedulableWithLock();
        if (candidates.isEmpty()) {
            log.debug("No schedulable email messages found");
            return;
        }

        //make a list of all customers who has an email address
        List<Customer> customers = customerRepository.findAllCustomersWithEmail().stream()
                .filter(Objects::nonNull)
                .filter(customer -> customer.getEmail() != null && !customer.getEmail().isBlank())
                .toList();

        if (customers.isEmpty()) {
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

            int successCount = sendEmailToAll(customers, subject, body);

            int totalRecipients = customers.size();
            int failedCount = Math.max(totalRecipients - successCount, 0);
            double emailSuccessRate = totalRecipients == 0
                    ? 0.0
                    : (successCount * 100.0) / totalRecipients;

            // if at least one email is successfully sent, save the message as a sent
            // message in the database
            if (successCount > 0) {
                sentMessageService.save(
                        toSentMessage(message, now, emailSuccessRate, totalRecipients, failedCount, successCount));
            }

            totalSuccess += successCount;

            // if at least one email is successfully sent, update lastEmailSentAt or
            // oneTimeEmailSent properties
            if (successCount > 0) {
                message.setLastEmailSentAt(now);
                if (isOneTime(message)) {
                    message.setOneTimeEmailSent(true);
                }
            }
        }

        // after all the due messages are processed, log the summary of this tick
        log.info("Scheduled email tick: candidates={}, due={}, recipients={}, successfulSends={}",
                candidates.size(), dueCount, customers.size(), totalSuccess);
    }

    // sends the due email to each of all the customers and returns the number of successful sends
    private int sendEmailToAll(List<Customer> customers, String subjectTemplate, String bodyTemplate) {
        int successCount = 0;

        String fromAddressForMail = resolveFromAddress();

        for (Customer customer : customers) {
            String toEmail = customer.getEmail() != null ? customer.getEmail().trim() : "";
            if (toEmail.isEmpty()) {
                continue;
            }

            //get the current bill of the relevant customer to replace placeholders in the email template
            Bill currentBill = billRepository.findTopByCustomerOrderByBillDateDesc(customer).orElse(null);
            
            String subject = replacePlaceholders(subjectTemplate, customer, currentBill);
            String body = replacePlaceholders(bodyTemplate, customer, currentBill);

            try {
                SimpleMailMessage mail = new SimpleMailMessage();

                if (!fromAddressForMail.isBlank()) {
                    mail.setFrom(fromAddressForMail);
                }
                mail.setTo(toEmail);
                mail.setSubject(subject);
                mail.setText(body);

                mailSender.send(mail);

                successCount++;
            } catch (Exception ex) {
                log.warn("Failed to send scheduled email to {}: {}", toEmail, ex.getMessage());
            }
        }

        return successCount;
    }

    private String resolveFromAddress() {
        if (fromAddress != null && !fromAddress.isBlank())
            return getFromAddress().trim();

        return "";
    }

    //replaces placeholders in the template with actual values from the relevant customer and their current bill, if available.
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

            LocalDate lastSentDate = message.getLastEmailSentAt() != null
                    ? message.getLastEmailSentAt().toLocalDate()
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
            // current time is before the scheduled time
            if (now.getDayOfMonth() == dayOfMonth) {
                return !now.toLocalTime().isBefore(message.getScheduleTime());
            }

            // if the current date is after the scheduled date, or if it is today and the
            // time is after the scheduled time, return true
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

        // if it is a custom template, return the content
        if (template.getContent() != null && !template.getContent().isBlank()) {
            return template.getContent();
        }

        // if there is nothing in content, but there are no template sections, return
        // empty string
        if (template.getSections() == null || template.getSections().isEmpty()) {
            return "";
        }

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
