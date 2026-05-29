package com.backend.water_management_system.messaging.service;

import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.messaging.entity.MessageTemplate;
import com.backend.water_management_system.messaging.entity.ScheduledMessage;
import com.backend.water_management_system.messaging.enums.MessageChannel;
import com.backend.water_management_system.messaging.enums.ScheduleType;
import com.backend.water_management_system.messaging.repository.ScheduledMessageRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ScheduledMessageDispatcherTest {

    @Mock
    private ScheduledMessageRepository scheduledMessageRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private SentMessageService sentMessageService;

    @Mock
    private MessageDispatchHelper dispatchHelper;

    @InjectMocks
    private ScheduledMessageDispatcher dispatcher;

    @Test
    void sendDueScheduledMessages_dispatchesSmsAndEmail() {
        ScheduledMessage message = new ScheduledMessage();
        message.setId(1L);
        message.setName("Monthly reminder");
        message.setScheduleType(ScheduleType.ONE_TIME);
        message.setScheduleDate(LocalDate.now());
        message.setScheduleTime(LocalTime.now().minusMinutes(1));
        message.setOneTimeMessageSent(false);
        message.setChannels(List.of(MessageChannel.SMS, MessageChannel.EMAIL));
        message.setSmsTemplate(new MessageTemplate());
        message.setEmailTemplate(new MessageTemplate());

        Customer customer = Customer.builder()
                .subscriptionNumber("SUB-1")
                .accountHolderName("Alex")
                .email("alex@example.com")
                .mobileNumber("0771234567")
                .build();

        given(dispatchHelper.canSendEmail()).willReturn(true);
        given(dispatchHelper.canSendSms()).willReturn(true);
        given(scheduledMessageRepository.findAllEmailSchedulableWithLock()).willReturn(List.of(message));
        given(customerRepository.findAll()).willReturn(List.of(customer));
        given(billRepository.findTopByCustomerOrderByBillDateDesc(customer)).willReturn(Optional.empty());
        given(dispatchHelper.buildSubject(message)).willReturn("Subject");
        given(dispatchHelper.buildBodyFromTemplate(any(MessageTemplate.class))).willReturn("Body");
        given(dispatchHelper.resolveFromAddress()).willReturn("from@example.com");
        given(dispatchHelper.resolveTemplateBody(anyString(), anyString())).willReturn("Body");
        given(dispatchHelper.isValidEmail("alex@example.com")).willReturn(true);
        given(dispatchHelper.dispatchSMS(any(), anyString(), anyString(), any(), any())).willReturn(true);
        given(dispatchHelper.dispatchEmail(any(), anyString(), anyString(), anyString(), anyString(), any(), any()))
                .willReturn(true);

        dispatcher.sendDueScheduledMessages();

        then(dispatchHelper).should().dispatchSMS(any(), anyString(), anyString(), any(), any());
        then(dispatchHelper).should().dispatchEmail(any(), anyString(), anyString(), anyString(), anyString(), any(),
                any());
        then(sentMessageService).should().save(any());
        assertThat(message.getLastMessageSentAt()).isNotNull();
        assertThat(message.getOneTimeMessageSent()).isTrue();
    }
}
