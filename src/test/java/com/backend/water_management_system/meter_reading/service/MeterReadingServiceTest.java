package com.backend.water_management_system.meter_reading.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.alerts.service.AlertService;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.billing.service.BillingService;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.meter_reading.dto.MeterReadingCreateRequest;
import com.backend.water_management_system.meter_reading.dto.MeterReadingTodayResponse;
import com.backend.water_management_system.meter_reading.entity.MeterReading;
import com.backend.water_management_system.meter_reading.repository.MeterReadingRepository;

@ExtendWith(MockitoExtension.class)
public class MeterReadingServiceTest {

    @Mock
    private MeterReadingRepository meterReadingRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private BillingService billingService;
    @Mock
    private BillRepository billRepository;
    @Mock
    private AlertService alertService;
    @Mock
    private ActivityAuditService activityAuditService;

    @InjectMocks
    private MeterReadingService meterReadingService;

    private Customer mockCustomer;
    private MeterReadingCreateRequest request;

    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setSubscriptionNumber("SUB123");
        mockCustomer.setAccountHolderName("John Doe");

        request = new MeterReadingCreateRequest();
        request.subscriptionNumber = "SUB123";
        request.meterNumber = "METER001";
        request.readingDate = LocalDate.now();
    }

    @Test
    void testSubmitReadingAndGenerateBill_WithUsageUnits() {
        // Arrange
        request.usageUnits = 50;
        when(customerRepository.findById("SUB123")).thenReturn(Optional.of(mockCustomer));
        when(meterReadingRepository.save(any(MeterReading.class))).thenAnswer(i -> i.getArgument(0));
        when(billingService.generateBill(any(Customer.class), any(MeterReading.class))).thenReturn(new Bill());

        // Act
        Bill result = meterReadingService.submitReadingAndGenerateBill(request);

        // Assert
        assertNotNull(result);
        verify(meterReadingRepository, times(1)).save(any(MeterReading.class));
        verify(alertService, times(1)).createAlert(eq("info"), anyString(), anyString(), eq("50 Units"), eq("SUB123"));
        verify(billingService, times(1)).generateBill(eq(mockCustomer), any(MeterReading.class));
    }

    @Test
    void testSubmitReadingAndGenerateBill_WithCalculatedUsage() {
        // Arrange
        request.previousReading = 1000;
        request.currentReading = 1150; // 150 units -> High usage
        when(customerRepository.findById("SUB123")).thenReturn(Optional.of(mockCustomer));
        when(meterReadingRepository.save(any(MeterReading.class))).thenAnswer(i -> i.getArgument(0));
        when(billingService.generateBill(any(Customer.class), any(MeterReading.class))).thenReturn(new Bill());

        // Act
        Bill result = meterReadingService.submitReadingAndGenerateBill(request);

        // Assert
        assertNotNull(result);
        verify(meterReadingRepository, times(1)).save(any(MeterReading.class));
        verify(alertService, times(1)).createAlert(eq("high"), anyString(), anyString(), eq("150 Units"), eq("SUB123"));
    }

    @Test
    void testSubmitReadingAndGenerateBill_InvalidReading() {
        // Arrange
        request.previousReading = 1000;
        request.currentReading = 900; // Invalid
        when(customerRepository.findById("SUB123")).thenReturn(Optional.of(mockCustomer));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            meterReadingService.submitReadingAndGenerateBill(request);
        });

        assertEquals("Current reading cannot be less than previous reading.", exception.getMessage());
        verify(meterReadingRepository, never()).save(any());
        verifyNoInteractions(activityAuditService);
    }

    @Test
    void creationAuditsOnlyAfterBillGenerationAndIgnoresSubmittedBy() {
        request.previousReading = 100;
        request.currentReading = 150;
        request.submittedBy = 999999L;
        when(customerRepository.findById("SUB123")).thenReturn(Optional.of(mockCustomer));
        when(meterReadingRepository.save(any())).thenAnswer(invocation -> {
            MeterReading reading = invocation.getArgument(0);
            reading.setReadingId(7L);
            return reading;
        });
        when(billingService.generateBill(any(), any())).thenReturn(new Bill());

        meterReadingService.submitReadingAndGenerateBill(request);

        InOrder order = inOrder(billingService, activityAuditService);
        order.verify(billingService).generateBill(eq(mockCustomer), any(MeterReading.class));
        order.verify(activityAuditService).recordAuthenticatedWeb(
                AuditAction.METER_READING_CREATED, AuditEntityType.METER_READING, 7L,
                Map.of("previousReading", "100", "currentReading", "150",
                        "usageUnits", "50", "readingDate", request.readingDate.toString()));
    }

    @Test
    void billGenerationFailureCreatesNoAuditEntry() {
        request.usageUnits = 20;
        when(customerRepository.findById("SUB123")).thenReturn(Optional.of(mockCustomer));
        when(meterReadingRepository.save(any())).thenAnswer(invocation -> {
            MeterReading reading = invocation.getArgument(0);
            reading.setReadingId(8L);
            return reading;
        });
        when(billingService.generateBill(any(), any())).thenThrow(new IllegalStateException("billing failed"));

        assertThrows(IllegalStateException.class,
                () -> meterReadingService.submitReadingAndGenerateBill(request));
        verifyNoInteractions(activityAuditService);
    }

    @Test
    void updateRecordsOnlySafeChangedValuesAfterBillUpdate() {
        MeterReading reading = reading(9L, 100, 150, 50, LocalDate.of(2026, 8, 1));
        MeterReadingCreateRequest update = new MeterReadingCreateRequest();
        update.previousReading = 110;
        update.currentReading = 180;
        update.usageUnits = 70;
        update.readingDate = LocalDate.of(2026, 8, 2);
        update.notes = "sensitive arbitrary note";
        update.submittedBy = 123456L;
        Bill bill = new Bill();
        when(meterReadingRepository.findById(9L)).thenReturn(Optional.of(reading));
        when(meterReadingRepository.save(reading)).thenReturn(reading);
        when(billRepository.findByMeterReading(reading)).thenReturn(Optional.of(bill));
        when(billingService.updateBill(bill, reading)).thenReturn(bill);

        meterReadingService.updateReading(9L, update);

        InOrder order = inOrder(billingService, activityAuditService);
        order.verify(billingService).updateBill(bill, reading);
        order.verify(activityAuditService).recordAuthenticatedWeb(
                AuditAction.METER_READING_UPDATED, AuditEntityType.METER_READING, 9L,
                Map.of("previousReading", "100 -> 110", "currentReading", "150 -> 180",
                        "usageUnits", "50 -> 70", "readingDate", "2026-08-01 -> 2026-08-02"));
    }

    @Test
    void noOpUpdateGeneratesBillButDoesNotCreateMisleadingAudit() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        MeterReading reading = reading(10L, 100, 150, 50, date);
        MeterReadingCreateRequest update = new MeterReadingCreateRequest();
        update.previousReading = 100;
        update.currentReading = 150;
        update.usageUnits = 50;
        update.readingDate = date;
        when(meterReadingRepository.findById(10L)).thenReturn(Optional.of(reading));
        when(meterReadingRepository.save(reading)).thenReturn(reading);
        when(billRepository.findByMeterReading(reading)).thenReturn(Optional.empty());
        when(billingService.generateBill(mockCustomer, reading)).thenReturn(new Bill());

        meterReadingService.updateReading(10L, update);

        verify(billingService).generateBill(mockCustomer, reading);
        verifyNoInteractions(activityAuditService);
    }

    @Test
    void testGetReadingsByDate() {
        // Arrange
        MeterReading reading = new MeterReading();
        reading.setReadingId(1L);
        reading.setCustomer(mockCustomer);
        reading.setReadingDate(LocalDate.now());

        Bill mockBill = new Bill();
        mockBill.setBillId(10L);
        mockBill.setStatus("PENDING");

        when(meterReadingRepository.findByReadingDate(LocalDate.now())).thenReturn(List.of(reading));
        when(billRepository.findByMeterReading(reading)).thenReturn(Optional.of(mockBill));

        // Act
        List<MeterReadingTodayResponse> responses = meterReadingService.getReadingsByDate(LocalDate.now());

        // Assert
        assertEquals(1, responses.size());
        assertEquals("John Doe", responses.get(0).customerName);
        assertEquals(10L, responses.get(0).billId);
        assertEquals("PENDING", responses.get(0).billStatus);
    }

    private MeterReading reading(Long id, int previous, int current, int usage, LocalDate date) {
        MeterReading reading = new MeterReading();
        reading.setReadingId(id);
        reading.setCustomer(mockCustomer);
        reading.setPreviousReading(previous);
        reading.setCurrentReading(current);
        reading.setUsageUnits(usage);
        reading.setReadingDate(date);
        return reading;
    }
}
