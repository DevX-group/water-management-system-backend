package com.backend.water_management_system.messaging.service;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.messaging.dto.SMSGatewayRequestDTO;
import com.backend.water_management_system.messaging.dto.SMSGatewayResponseDTO;
import com.backend.water_management_system.messaging.entity.Message;
import com.backend.water_management_system.messaging.entity.MessageTemplate;
import com.backend.water_management_system.payments.entity.Payment;

import jakarta.annotation.PostConstruct;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class MessageDispatchHelper {

    private static final Logger log = LoggerFactory.getLogger(MessageDispatchHelper.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.%-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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

    @PostConstruct
    private void initMailSender() {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.webClient = WebClient.create();
    }

    public boolean canSendEmail() {
        return mailSender != null;
    }

    public boolean canSendSms() {
        return textLkApiEndpoint != null && !textLkApiEndpoint.isBlank()
                && textLkApiToken != null && !textLkApiToken.isBlank();
    }

    public String resolveFromAddress() {
        if (fromAddress != null && !fromAddress.isBlank()) {
            return fromAddress.trim();
        }

        return "";
    }

    // dispatches a due scheduled message or a triggered message to a single customer as a SMS
    public boolean dispatchSMS(Customer customer, String toPhone, String smsTemplateToUse, Bill currentBill, Payment payment) {
        String smsBody = messagePlaceholderService.replacePlaceholders(smsTemplateToUse, customer, currentBill,
                payment);

        boolean smsOk = sendSms(toPhone, smsBody);

        return smsOk;
    }

    // dispatches a due message to a single customer as an email placeholders
    public boolean dispatchEmail(Customer customer,
            String toEmail,
            String fromAddressForMail,
            String subjectTemplate,
            String emailTemplateToUse,
            Bill currentBill,
            Payment payment) {

        String subject = messagePlaceholderService.replacePlaceholders(subjectTemplate, customer, currentBill, payment);
        String body = messagePlaceholderService.replacePlaceholders(emailTemplateToUse, customer, currentBill, payment);

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
            log.warn("Failed to send email to {}: {}", toEmail, ex.getMessage());
            return false;
        }
    }

    // Sends SMS using Text.lk gateway. Returns true if the gateway returned a
    // successful response.
    public boolean sendSms(String to, String message) {

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
            log.warn("Failed to send SMS to {}: {}", to, ex.getMessage());
            return false;
        }
    }

    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // if the template body is not available for the relevant channel, replace it
    // with the template body of the other channel
    public String resolveTemplateBody(String primaryTemplate, String fallbackTemplate) {
        return (primaryTemplate != null && !primaryTemplate.isBlank() ? primaryTemplate : fallbackTemplate);
    }

    public String buildSubject(Message message) {
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

    public String buildBodyFromTemplate(MessageTemplate template) {
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
}
