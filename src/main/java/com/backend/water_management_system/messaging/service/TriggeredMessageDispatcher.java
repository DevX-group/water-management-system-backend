package com.backend.water_management_system.messaging.service;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.messaging.entity.TriggeredMessage;
import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.TriggerType;
import com.backend.water_management_system.messaging.repository.TriggeredMessageRepository;
import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.customer.repository.CustomerRepository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TriggeredMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TriggeredMessageDispatcher.class);

    private final TriggeredMessageRepository triggeredMessageRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final MessageDispatchHelper dispatchHelper;

    @Async
    public void dispatchPaymentConfirmed(Payment payment) {
        if (payment == null) {
            return;
        }

        if (payment.getPaymentMethod() != PaymentMethod.MANUAL
                && payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER
                && payment.getPaymentMethod() != PaymentMethod.ONLINE) {
            return;
        }

        boolean canSendEmail = dispatchHelper.canSendEmail();
        boolean canSendSms = dispatchHelper.canSendSms();

        if (!canSendEmail && !canSendSms) {
            log.warn("No MailSender or SMS gateway configured; skipping triggered message dispatch");
            return;
        }

        List<TriggeredMessage> messages = triggeredMessageRepository
                .findByTriggerTypeAndActiveTrue(TriggerType.PAYMENT_CONFIRMED);

        if (messages.isEmpty()) {
            return;
        }

        Customer customer = customerRepository.findById(payment.getSubscriptionNumber()).orElse(null);
        if (customer == null) {
            log.warn("Customer not found for payment confirmation: {}", payment.getSubscriptionNumber());
            return;
        }

        Bill currentBill = billRepository.findTopByCustomerOrderByBillDateDesc(customer).orElse(null);

        for (TriggeredMessage message : messages) {
            List<MessageChannel> channels = message.getChannels();
            boolean shouldSendSMS = channels != null && channels.contains(MessageChannel.SMS);
            boolean shouldSendEmail = channels != null && channels.contains(MessageChannel.EMAIL);

            String subjectTemplate = dispatchHelper.buildSubject(message);
            String emailBodyTemplate = dispatchHelper.buildBodyFromTemplate(message.getEmailTemplate());
            String smsBodyTemplate = dispatchHelper.buildBodyFromTemplate(message.getSmsTemplate());
            String fromAddressForMail = dispatchHelper.resolveFromAddress();

            if (shouldSendSMS && canSendSms) {
                String toPhone = customer.getMobileNumber() != null ? customer.getMobileNumber().trim() : "";
                if (!toPhone.isEmpty()) {
                    String smsTemplateToUse = dispatchHelper.resolveTemplateBody(smsBodyTemplate, emailBodyTemplate);
                    dispatchHelper.dispatchSMS(customer, toPhone, smsTemplateToUse, currentBill, payment);
                }
            }

            if (shouldSendEmail && canSendEmail) {
                String toEmail = customer.getEmail() != null ? customer.getEmail().trim() : "";
                if (dispatchHelper.isValidEmail(toEmail)) {
                    String emailTemplateToUse = dispatchHelper.resolveTemplateBody(emailBodyTemplate, smsBodyTemplate);
                    dispatchHelper.dispatchEmail(customer, toEmail, fromAddressForMail, subjectTemplate,
                            emailTemplateToUse, currentBill, payment);
                }
            }
        }
    }
}
