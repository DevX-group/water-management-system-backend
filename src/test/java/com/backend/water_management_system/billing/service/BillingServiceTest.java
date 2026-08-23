package com.backend.water_management_system.billing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.common.entity.ConnectionRate;
import com.backend.water_management_system.common.repository.RateRepository;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.meter_reading.entity.MeterReading;
import com.backend.water_management_system.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
public class BillingServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private RateRepository rateRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BillingService billingService;

    private Customer mockCustomer;
    private MeterReading mockReading;
    private ConnectionRate mockRate;

    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setSubscriptionNumber("SUB123");
        mockCustomer.setConnectionType("metered");

        mockReading = new MeterReading();
        mockReading.setUsageUnits(120);

        mockRate = new ConnectionRate();
        mockRate.setConnectionType("metered");
        mockRate.setBaseRate(500.0);
        mockRate.setUnitRateTier1(10.0);
        mockRate.setUnitRateTier2(15.0);
        mockRate.setUnitRateTier3(20.0);
        mockRate.setTier1Limit(50);
        mockRate.setTier2Limit(100);
        mockRate.setTaxRate(0.15);
    }

    @Test
    void testGenerateBill_MeteredConnection() {
        // Arrange
        when(rateRepository.findById("metered")).thenReturn(Optional.of(mockRate));
        when(billRepository.getTotalPendingBalance("SUB123")).thenReturn(BigDecimal.ZERO);
        
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> {
            Bill savedBill = invocation.getArgument(0);
            savedBill.setBillId(1L);
            return savedBill;
        });

        // Act
        Bill generatedBill = billingService.generateBill(mockCustomer, mockReading);

        // Assert
        assertNotNull(generatedBill);
        assertEquals(120, generatedBill.getUsageUnits());
        assertEquals(new BigDecimal("500.0"), generatedBill.getBaseCharge());
        
        // Tier 1: 50 units * 10 = 500
        // Tier 2: 50 units * 15 = 750
        // Tier 3: 20 units * 20 = 400
        // Usage Charge = 1650
        assertEquals(new BigDecimal("1650.0"), generatedBill.getUsageCharge());
        
        // Subtotal = 500 + 1650 = 2150
        // Tax = 2150 * 0.15 = 322.50
        // Total = 2472.50
        assertEquals(new BigDecimal("322.50"), generatedBill.getTaxAmount());
        assertEquals(new BigDecimal("2472.50"), generatedBill.getTotalAmount());
        
        verify(rateRepository, times(1)).findById("metered");
        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void testGenerateBill_RateNotFound() {
        // Arrange
        when(rateRepository.findById("metered")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            billingService.generateBill(mockCustomer, mockReading);
        });

        assertEquals("Rates not found in DB for: metered", exception.getMessage());
        verify(billRepository, never()).save(any(Bill.class));
    }
}
