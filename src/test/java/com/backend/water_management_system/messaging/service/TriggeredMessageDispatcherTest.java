package com.backend.water_management_system.messaging.service;

import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.messaging.entity.MessageTemplate;
import com.backend.water_management_system.messaging.entity.TriggeredMessage;
import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.TriggerType;
import com.backend.water_management_system.messaging.repository.TriggeredMessageRepository;
import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TriggeredMessageDispatcherTest {

    @Mock
    private TriggeredMessageRepository triggeredMessageRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private MessageDispatchHelper dispatchHelper;

    @InjectMocks
    private TriggeredMessageDispatcher dispatcher;

    @Test
    void dispatchPaymentConfirmed_sendsSmsAndEmail() {
        TriggeredMessage message = new TriggeredMessage();
        message.setName("Payment confirmed");
        message.setTriggerType(TriggerType.PAYMENT_CONFIRMED);
        message.setChannels(List.of(MessageChannel.SMS, MessageChannel.EMAIL));
        message.setSmsTemplate(new MessageTemplate());
        message.setEmailTemplate(new MessageTemplate());
        message.setActive(true);

        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.ONLINE);
        payment.setSubscriptionNumber("SUB-1");

        Customer customer = Customer.builder()
                .subscriptionNumber("SUB-1")
                .accountHolderName("Alex")
                .email("alex@example.com")
                .mobileNumber("0771234567")
                .build();

        given(dispatchHelper.canSendEmail()).willReturn(true);
        given(dispatchHelper.canSendSms()).willReturn(true);
        given(triggeredMessageRepository.findByTriggerTypeAndActiveTrue(TriggerType.PAYMENT_CONFIRMED))
                .willReturn(List.of(message));
        given(customerRepository.findById("SUB-1")).willReturn(Optional.of(customer));
        given(billRepository.findTopByCustomerOrderByBillDateDesc(customer)).willReturn(Optional.empty());
        given(dispatchHelper.buildSubject(message)).willReturn("Subject");
        given(dispatchHelper.buildBodyFromTemplate(any(MessageTemplate.class))).willReturn("Body");
        given(dispatchHelper.resolveFromAddress()).willReturn("from@example.com");
        given(dispatchHelper.resolveTemplateBody(anyString(), anyString())).willReturn("Body");
        given(dispatchHelper.isValidEmail("alex@example.com")).willReturn(true);

        dispatcher.dispatchPaymentConfirmed(payment);

        then(dispatchHelper).should().dispatchSMS(any(), anyString(), anyString(), any(), any());
        then(dispatchHelper).should().dispatchEmail(any(), anyString(), anyString(), anyString(), anyString(), any(),
                any());
    }
}
