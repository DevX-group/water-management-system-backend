package com.backend.water_management_system.messaging.service;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.messaging.entity.TriggeredMessage;
import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.TriggerType;
import com.backend.water_management_system.messaging.repository.TriggeredMessageRepository;
import com.backend.water_management_system.payments.entity.BankSlip;
import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.meter_reading.entity.MeterReading;
import com.backend.water_management_system.settings.entity.SystemDetails;
import com.backend.water_management_system.settings.service.SystemSettingsService;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TriggeredMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TriggeredMessageDispatcher.class);

    private final TriggeredMessageRepository triggeredMessageRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final MessageDispatchHelper dispatchHelper;
    private final SystemSettingsService systemSettingsService;

    // for confirmed payments
    public void dispatchPaymentConfirmed(Payment payment) {
        if (payment == null) {
            return;
        }

        if (payment.getPaymentMethod() != PaymentMethod.MANUAL
                && payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER
                && payment.getPaymentMethod() != PaymentMethod.ONLINE) {
            return;
        }

        dispatchTriggeredMessage(TriggerType.PAYMENT_CONFIRMED, payment);
    }

    // for rejected bank slips
    public void dispatchBankSlipRejected(BankSlip bankSlip) {
        if (bankSlip == null) {
            return;
        }

        dispatchTriggeredMessage(TriggerType.BANK_SLIP_REJECTED, null, bankSlip, null, null, null);
    }

    // Sends the monthly bill message immediately after a new meter reading creates
    // a bill.
    public void dispatchBillAndOverdue(MeterReading reading, Bill bill, Customer customer) {
        dispatchTriggeredMessage(TriggerType.BILL_AND_OVERDUE, null, null, reading, bill, customer);
    }

    // Kept as the stable dispatcher entry point for existing payment flows and
    // tests.
    public void dispatchTriggeredMessage(TriggerType triggerType, Payment payment) {
        dispatchTriggeredMessage(triggerType, payment, payment != null ? payment.getBankSlip() : null,
                null, null, null);
    }

    private void dispatchTriggeredMessage(TriggerType triggerType, Payment payment, BankSlip bankSlip,
            MeterReading meterReading, Bill currentBill, Customer customer) {
        // Payment and bank-slip triggers resolve their customer and latest bill here.
        // The bill trigger already supplies both objects from the meter-reading flow.
        if (payment != null || bankSlip != null) {
            String subscriptionNumber = payment != null ? payment.getSubscriptionNumber()
                    : bankSlip.getSubscriptionNumber();
            if (subscriptionNumber == null || subscriptionNumber.isBlank()) {
                return;
            }

            customer = customerRepository.findById(subscriptionNumber).orElse(null);
            if (customer == null) {
                log.warn("Customer not found for triggered message {}: {}", triggerType, subscriptionNumber);
                return;
            }

            currentBill = billRepository.findTopByCustomerOrderByBillDateDesc(customer).stream()
                    .findFirst()
                    .orElse(null);
        }

        if (customer == null) {
            return;
        }

        boolean canSendEmail = dispatchHelper.canSendEmail();
        boolean canSendSms = dispatchHelper.canSendSms();

        if (!canSendEmail && !canSendSms) {
            log.warn("No MailSender or SMS gateway configured; skipping triggered message dispatch");
            return;
        }

        List<TriggeredMessage> messages = triggeredMessageRepository
                .findByTriggerTypeAndActiveTrue(triggerType);

        if (messages.isEmpty()) {
            return;
        }

        SystemDetails systemDetails = systemSettingsService.findSystemDetails();

        BigDecimal overdueThreshold = systemDetails.getOverdueThreshold();
        log.info("Overdue threshold: {}", overdueThreshold);

        boolean exceedsOverdueThreshold = currentBill != null
                && currentBill.getOutstandingAtIssue() != null
                && overdueThreshold != null
                && currentBill.getOutstandingAtIssue().compareTo(overdueThreshold) > 0;
        log.info("Outstanding value: {}", currentBill.getOutstandingAtIssue());

        for (TriggeredMessage message : messages) {
            List<MessageChannel> channels = message.getChannels();
            boolean shouldSendSMS = channels != null && channels.contains(MessageChannel.SMS);
            boolean shouldSendEmail = channels != null && channels.contains(MessageChannel.EMAIL);

            String subjectTemplate = dispatchHelper.buildSubject(message);
            String emailBodyTemplate = dispatchHelper.buildBodyFromTemplate(message.getEmailTemplate());
            String smsBodyTemplate = dispatchHelper.buildBodyFromTemplate(message.getSmsTemplate());
            if (exceedsOverdueThreshold) {
                emailBodyTemplate = appendTemplate(emailBodyTemplate,
                        dispatchHelper.buildBodyFromTemplate(message.getOverdueAlertEmailTemplate()));
                smsBodyTemplate = appendTemplate(smsBodyTemplate,
                        dispatchHelper.buildBodyFromTemplate(message.getOverdueAlertSmsTemplate()));
            }
            String fromAddressForMail = dispatchHelper.resolveFromAddress();

            if (shouldSendSMS && canSendSms) {
                String toPhone = customer.getMobileNumber() != null ? customer.getMobileNumber().trim() : "";
                if (!toPhone.isEmpty()) {
                    String smsTemplateToUse = dispatchHelper.resolveTemplateBody(smsBodyTemplate, emailBodyTemplate);
                    dispatchHelper.dispatchSMS(customer, toPhone, smsTemplateToUse, currentBill, payment, bankSlip,
                            meterReading);
                }
            }

            if (shouldSendEmail && canSendEmail) {
                String toEmail = customer.getEmail() != null ? customer.getEmail().trim() : "";
                if (dispatchHelper.isValidEmail(toEmail)) {
                    String emailTemplateToUse = dispatchHelper.resolveTemplateBody(emailBodyTemplate, smsBodyTemplate);
                    dispatchHelper.dispatchEmail(customer, toEmail, fromAddressForMail, subjectTemplate,
                            emailTemplateToUse, currentBill, payment, bankSlip, meterReading);
                }
            }
        }
    }

    private String appendTemplate(String mainTemplate, String alertTemplate) {
        if (alertTemplate == null || alertTemplate.isBlank()) {
            return mainTemplate;
        }
        if (mainTemplate == null || mainTemplate.isBlank()) {
            return alertTemplate;
        }
        return mainTemplate + "\n\n" + alertTemplate;
    }
}
