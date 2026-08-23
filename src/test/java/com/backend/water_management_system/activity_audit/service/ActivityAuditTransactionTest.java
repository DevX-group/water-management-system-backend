package com.backend.water_management_system.activity_audit.service;

import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.repository.ActivityAuditLogRepository;
import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentStatus;
import com.backend.water_management_system.payments.repository.PaymentRepository;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.meter_reading.entity.MeterReading;
import com.backend.water_management_system.meter_reading.repository.MeterReadingRepository;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({ActivityAuditService.class, AuditActorResolver.class,
        SafeChangedFieldsSerializer.class, ObjectMapper.class})
class ActivityAuditTransactionTest {

    @Autowired
    private ActivityAuditService auditService;
    @Autowired
    private ActivityAuditLogRepository auditRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private MeterReadingRepository meterReadingRepository;
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void callerRollbackRemovesBusinessAndAuditRows() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        UUID[] userId = new UUID[1];

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            User savedUser = userRepository.save(User.builder()
                    .nic("test-nic-transaction")
                    .email("transaction@example.test")
                    .fullName("Transaction User")
                    .role(Role.CUSTOMER)
                    .status(UserStatus.PENDING_ACTIVATION)
                    .build());
            userId[0] = savedUser.getId();
            auditService.recordSystem(
                    AuditAction.USER_CREATED, AuditEntityType.USER, userId[0],
                    Map.of("role", "CUSTOMER", "status", "PENDING_ACTIVATION"));
            throw new IllegalStateException("force caller rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(userRepository.findById(userId[0])).isEmpty();
        assertThat(auditRepository.count()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void auditPersistenceFailureRollsBackAssociatedBusinessMutation() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        UUID[] userId = new UUID[1];

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            User savedUser = userRepository.save(User.builder()
                    .nic("rollback-nic")
                    .email("rollback@example.test")
                    .role(Role.CUSTOMER)
                    .status(UserStatus.PENDING_ACTIVATION)
                    .build());
            userId[0] = savedUser.getId();
            auditService.recordSystem(
                    AuditAction.USER_CREATED, AuditEntityType.USER, userId[0],
                    Map.of("unsafePersonalField", "must fail"));
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(userRepository.findById(userId[0])).isEmpty();
        assertThat(auditRepository.count()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void auditFailureRollsBackPaymentMutation() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String paymentId = "PAY-ROLLBACK";

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            paymentRepository.save(Payment.builder()
                    .paymentId(paymentId)
                    .amount(new java.math.BigDecimal("25"))
                    .paymentMethod(PaymentMethod.MANUAL)
                    .status(PaymentStatus.FULL)
                    .build());
            auditService.recordSystem(
                    AuditAction.PAYMENT_CREATED, AuditEntityType.PAYMENT, paymentId,
                    Map.of("unsafePaymentField", "must fail"));
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(paymentRepository.findById(paymentId)).isEmpty();
        assertThat(auditRepository.count()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void auditFailureRollsBackMeterReadingAndBillMutations() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long[] readingId = new Long[1];
        Long[] billId = new Long[1];

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            MeterReading reading = new MeterReading();
            reading.setPreviousReading(100);
            reading.setCurrentReading(125);
            reading.setUsageUnits(25);
            reading.setReadingDate(java.time.LocalDate.of(2026, 8, 1));
            MeterReading savedReading = meterReadingRepository.save(reading);
            readingId[0] = savedReading.getReadingId();

            Bill bill = new Bill();
            bill.setMeterReading(savedReading);
            Bill savedBill = billRepository.save(bill);
            billId[0] = savedBill.getBillId();

            auditService.recordSystem(
                    AuditAction.METER_READING_CREATED, AuditEntityType.METER_READING,
                    readingId[0], Map.of("unsafeReadingField", "must fail"));
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(meterReadingRepository.findById(readingId[0])).isEmpty();
        assertThat(billRepository.findById(billId[0])).isEmpty();
        assertThat(auditRepository.count()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void billingFailureRollsBackMeterReadingAndCreatesNoAudit() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long[] readingId = new Long[1];

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            MeterReading reading = new MeterReading();
            reading.setUsageUnits(25);
            MeterReading savedReading = meterReadingRepository.save(reading);
            readingId[0] = savedReading.getReadingId();
            throw new IllegalStateException("simulated billing failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(meterReadingRepository.findById(readingId[0])).isEmpty();
        assertThat(auditRepository.count()).isZero();
    }
}
