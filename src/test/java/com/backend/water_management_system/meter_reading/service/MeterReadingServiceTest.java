package com.backend.water_management_system.meter_reading.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.alerts.service.AlertService;
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
        verify(alertService, times(1)).createAlert(eq("info"), anyString(), anyString(), eq("50 Units"));
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
        verify(alertService, times(1)).createAlert(eq("high"), anyString(), anyString(), eq("150 Units"));
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
    }

    @Test
    void testGetTodaysReadings() {
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
        List<MeterReadingTodayResponse> responses = meterReadingService.getTodaysReadings();

        // Assert
        assertEquals(1, responses.size());
        assertEquals("John Doe", responses.get(0).customerName);
        assertEquals(10L, responses.get(0).billId);
        assertEquals("PENDING", responses.get(0).billStatus);
    }
}
