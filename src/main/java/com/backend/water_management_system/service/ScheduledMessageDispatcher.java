package com.backend.water_management_system.service;

import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.MessageTemplate;
import com.backend.water_management_system.entity.ScheduledMessage;
import com.backend.water_management_system.entity.SentMessage;
import com.backend.water_management_system.entity.SentMessageFailure;
import com.backend.water_management_system.entity.TemplateSection;
import com.backend.water_management_system.repository.BillRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.ScheduledMessageRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduledMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledMessageDispatcher.class);

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final SentMessageService sentMessageService;
    private final MessageDispatchHelper dispatchHelper;

    @Scheduled(fixedDelayString = "${app.messaging.scheduler-delay-ms:60000}")
    @Transactional
    public void sendDueScheduledMessages() {
        boolean canSendEmail = dispatchHelper.canSendEmail();
        boolean canSendSms = dispatchHelper.canSendSms();

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

        String subjectTemplate = dispatchHelper.buildSubject(message);
        String emailBodyTemplate = dispatchHelper.buildBodyFromTemplate(message.getEmailTemplate());
        String smsBodyTemplate = dispatchHelper.buildBodyFromTemplate(message.getSmsTemplate());

        String fromAddressForMail = dispatchHelper.resolveFromAddress();

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
                    String smsTemplateToUse = dispatchHelper.resolveTemplateBody(smsBodyTemplate, emailBodyTemplate);

                    boolean smsOk = dispatchHelper.dispatchSMS(customer, toPhone, smsTemplateToUse, currentBill, null);

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
                if (dispatchHelper.isValidEmail(toEmail)) {
                    emailAttempted = true;
                    counts.totalEmails++;
                    String emailTemplateToUse = dispatchHelper.resolveTemplateBody(emailBodyTemplate, smsBodyTemplate);

                    boolean emailOk = dispatchHelper.dispatchEmail(customer, toEmail, fromAddressForMail,
                            subjectTemplate,
                            emailTemplateToUse, currentBill, null);

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
