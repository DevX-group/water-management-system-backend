package com.backend.water_management_system.payments.service;

import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.common.entity.Region;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.customer.service.CustomerAccessService;
import com.backend.water_management_system.messaging.service.TriggeredMessageDispatcher;
import com.backend.water_management_system.notification.service.NotificationService;
import com.backend.water_management_system.payments.config.PayHereConfig;
import com.backend.water_management_system.payments.dto.AddPaymentRequest;
import com.backend.water_management_system.payments.dto.BankSlipActionRequest;
import com.backend.water_management_system.payments.dto.CustomerAddPaymentRequest;
import com.backend.water_management_system.payments.dto.PaymentResult;
import com.backend.water_management_system.payments.entity.BankSlip;
import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentStatus;
import com.backend.water_management_system.payments.enums.PaymentType;
import com.backend.water_management_system.payments.enums.SlipStatus;
import com.backend.water_management_system.payments.repository.BankSlipRepository;
import com.backend.water_management_system.payments.repository.PaymentAllocationRepository;
import com.backend.water_management_system.payments.repository.PaymentRepository;
import com.backend.water_management_system.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentActivityAuditTest {

    @Mock CustomerRepository customerRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock BillRepository billRepository;
    @Mock PaymentAllocationRepository allocationRepository;
    @Mock TriggeredMessageDispatcher messageDispatcher;
    @Mock NotificationService notificationService;
    @Mock ActivityAuditService auditService;
    @Mock BankSlipRepository bankSlipRepository;
    @Mock CloudinaryService cloudinaryService;
    @Mock CustomerAccessService customerAccessService;
    @Mock SimpMessagingTemplate messagingTemplate;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = spy(new PaymentService(customerRepository, paymentRepository, billRepository,
                allocationRepository, messageDispatcher, notificationService, auditService));
    }

    @Test
    void manualCreationRecordsOneAuthenticatedSafeEntryAndFailureRecordsNone() {
        AddPaymentRequest request = AddPaymentRequest.builder().subscriptionNumber("SUB-1")
                .amount(new BigDecimal("100.00")).paymentType(PaymentType.MONTHLY)
                .paymentMethod(PaymentMethod.MANUAL).build();
        when(customerRepository.findById("SUB-1")).thenReturn(Optional.of(new Customer()));
        Bill bill = new Bill();
        bill.setBalanceDue(new BigDecimal("100"));
        bill.setTotalAmount(new BigDecimal("100"));
        doReturn(bill).when(paymentService).getLatestMonthlyBill("SUB-1");
        doReturn(new PaymentResult(BigDecimal.TEN, BigDecimal.ZERO, PaymentStatus.FULL))
                .when(paymentService).processMonthlyPayment(any(), eq(new BigDecimal("100.00")), eq(bill));

        paymentService.addPayment(request);

        verify(auditService).recordAuthenticatedWeb(eq(AuditAction.PAYMENT_CREATED),
                eq(AuditEntityType.PAYMENT), anyString(), eq(Map.of(
                        "amount", "100", "paymentType", "MONTHLY",
                        "paymentMethod", "MANUAL", "status", "FULL")));

        clearInvocations(auditService);
        assertThatThrownBy(() -> paymentService.addPayment(AddPaymentRequest.builder().build()))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(auditService);
    }

    @Test
    void updateRecordsOldAndNewValuesWhileNoOpIsSilent() {
        Payment payment = payment("PAY-1", "50", PaymentStatus.PARTIAL, PaymentMethod.MANUAL);
        payment.setPaymentType(PaymentType.MONTHLY);
        when(paymentRepository.findById("PAY-1")).thenReturn(Optional.of(payment));
        doNothing().when(paymentService).reversePaymentEffect(payment);
        doReturn(PaymentStatus.FULL).when(paymentService).applyPaymentEffect(payment);

        paymentService.updatePayment("PAY-1", new BigDecimal("75"));
        verify(auditService).recordAuthenticatedWeb(AuditAction.PAYMENT_UPDATED,
                AuditEntityType.PAYMENT, "PAY-1",
                Map.of("amount", "50 -> 75", "status", "PARTIAL -> FULL"));

        clearInvocations(auditService);
        doReturn(PaymentStatus.FULL).when(paymentService).applyPaymentEffect(payment);
        paymentService.updatePayment("PAY-1", new BigDecimal("75"));
        verifyNoInteractions(auditService);
    }

    @Test
    void deletionRecordsAfterDeleteWithSafeSnapshot() {
        Payment payment = payment("PAY-1", "40", PaymentStatus.FULL, PaymentMethod.MANUAL);
        payment.setPaymentType(PaymentType.MONTHLY);
        when(paymentRepository.findById("PAY-1")).thenReturn(Optional.of(payment));
        doNothing().when(paymentService).reversePaymentEffect(payment);

        paymentService.deletePayment("PAY-1");

        verify(paymentRepository).delete(payment);
        verify(auditService).recordAuthenticatedWeb(AuditAction.PAYMENT_DELETED,
                AuditEntityType.PAYMENT, "PAY-1", Map.of(
                        "amount", "40", "paymentType", "MONTHLY",
                        "paymentMethod", "MANUAL", "status", "FULL"));
    }

    @Test
    void onlineInitiationAndCallbackUseAuthenticatedThenSystemAndAreIdempotent() {
        PayHereConfig config = payHereConfig();
        CustomerPaymentService customerService = spy(new CustomerPaymentService(customerRepository,
                paymentRepository, paymentService, config, billRepository, messageDispatcher, auditService));
        Customer customer = Customer.builder().accountHolderName("Test User").address("Address")
                .region(Region.builder().regionCode("R1").regionName("City").build())
                .user(User.builder().email("person@example.test").phoneNumber("0712345678").build()).build();
        when(billRepository.getTotalPendingBalance("SUB-1")).thenReturn(new BigDecimal("100"));
        when(customerRepository.findBySubscriptionNumber("SUB-1")).thenReturn(Optional.of(customer));

        customerService.initiateCustomerPayment(CustomerAddPaymentRequest.builder()
                .amount(new BigDecimal("25")).paymentMethod(PaymentMethod.ONLINE).build(), "SUB-1");
        verify(auditService).recordAuthenticatedWeb(eq(AuditAction.PAYMENT_CREATED),
                eq(AuditEntityType.PAYMENT), anyString(), eq(Map.of(
                        "amount", "25", "paymentMethod", "ONLINE", "status", "PENDING")));

        clearInvocations(auditService);
        Payment pending = payment("PAY-2", "25", PaymentStatus.PENDING, PaymentMethod.ONLINE);
        pending.setOrderId("ORDER-2");
        when(paymentRepository.findByOrderId("ORDER-2")).thenReturn(Optional.of(pending));
        doReturn(PaymentStatus.FULL).when(customerService).processPayment(pending);
        Map<String, String> callback = callback(config, "ORDER-2", "25.00", "2");

        customerService.handlePayhereNotification(callback);
        verify(auditService).recordSystem(AuditAction.PAYMENT_STATUS_CHANGED,
                AuditEntityType.PAYMENT, "PAY-2", Map.of("status", "PENDING -> FULL"));

        clearInvocations(auditService);
        customerService.handlePayhereNotification(callback);
        verifyNoInteractions(auditService);
    }

    @Test
    void cleanupAuditsOnlyPaymentsThatActuallyTransition() {
        Payment pending = payment("PAY-1", "10", PaymentStatus.PENDING, PaymentMethod.ONLINE);
        Payment alreadyExpired = payment("PAY-2", "10", PaymentStatus.EXPIRED, PaymentMethod.ONLINE);
        when(paymentRepository.findByStatusAndPaymentMethodAndCreatedAtBefore(
                eq(PaymentStatus.PENDING), eq(PaymentMethod.ONLINE), any())).thenReturn(List.of(pending, alreadyExpired));
        PaymentCleanupService cleanup = new PaymentCleanupService(paymentRepository, auditService);

        cleanup.expireOldPendingPayments();

        verify(auditService).recordSystem(AuditAction.PAYMENT_STATUS_CHANGED,
                AuditEntityType.PAYMENT, "PAY-1", Map.of("status", "PENDING -> EXPIRED"));
        verify(auditService, times(1)).recordSystem(any(), any(), any(), any());
    }

    @Test
    void approvedBankSlipCreatesExactlyOneAuthenticatedPaymentEntry() {
        PayHereConfig config = payHereConfig();
        CustomerPaymentService customerService = mock(CustomerPaymentService.class);
        BankSlipService bankSlipService = new BankSlipService(cloudinaryService, customerService,
                notificationService, bankSlipRepository, customerRepository, customerAccessService,
                paymentRepository, billRepository, messageDispatcher, auditService, messagingTemplate);
        BankSlip slip = BankSlip.builder().slipId(1L).subscriptionNumber("SUB-1")
                .amount(new BigDecimal("30")).status(SlipStatus.PENDING).build();
        when(bankSlipRepository.findById(1L)).thenReturn(Optional.of(slip));
        when(customerRepository.findBySubscriptionNumber("SUB-1")).thenReturn(Optional.of(new Customer()));
        when(customerService.processPayment(any())).thenReturn(PaymentStatus.FULL);
        BankSlipActionRequest request = new BankSlipActionRequest();
        request.setSlipId(1L);
        request.setAction(SlipStatus.APPROVED);

        bankSlipService.processBankSlipReview(request);

        verify(auditService, times(1)).recordAuthenticatedWeb(eq(AuditAction.PAYMENT_CREATED),
                eq(AuditEntityType.PAYMENT), anyString(), eq(Map.of(
                        "amount", "30", "paymentMethod", "BANK_TRANSFER", "status", "FULL")));
    }

    private static Payment payment(String id, String amount, PaymentStatus status, PaymentMethod method) {
        return Payment.builder().paymentId(id).subscriptionNumber("SUB-1")
                .amount(new BigDecimal(amount)).status(status).paymentMethod(method)
                .createdAt(LocalDateTime.now()).build();
    }

    private static PayHereConfig payHereConfig() {
        PayHereConfig config = new PayHereConfig();
        config.setMerchantId("merchant");
        config.setMerchantSecret("secret");
        config.setReturnUrl("return");
        config.setCancelUrl("cancel");
        config.setNotifyUrl("notify");
        return config;
    }

    private static Map<String, String> callback(PayHereConfig config, String orderId,
            String amount, String status) {
        String signature = md5(config.getMerchantId() + orderId + amount + "LKR" + status
                + md5(config.getMerchantSecret()));
        return Map.of("order_id", orderId, "payment_id", "gateway-id", "status_code", status,
                "md5sig", signature, "payhere_amount", amount);
    }

    private static String md5(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                    .digest(value.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
