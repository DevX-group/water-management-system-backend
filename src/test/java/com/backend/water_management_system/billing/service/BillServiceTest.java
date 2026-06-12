package com.backend.water_management_system.billing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.billing.dto.BillResponse;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;

@ExtendWith(MockitoExtension.class)
public class BillServiceTest {

    @Mock
    private BillRepository billRepository;

    @InjectMocks
    private BillService billService;

    private Bill mockBill;

    @BeforeEach
    void setUp() {
        mockBill = new Bill();
        mockBill.setBillId(1L);
        mockBill.setBillingPeriod("2023-10");
        mockBill.setBillDate(LocalDate.now());
        mockBill.setDueDate(LocalDate.now().plusDays(30));
        mockBill.setUsageUnits(150);
        mockBill.setTotalAmount(new BigDecimal("1500.00"));
        mockBill.setBalanceDue(new BigDecimal("1500.00"));
        mockBill.setStatus("PENDING");
    }

    @Test
    void testGetBillsForCustomer() {
        // Arrange
        String subscriptionNumber = "SUB123";
        when(billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber))
                .thenReturn(List.of(mockBill));

        // Act
        List<BillResponse> responses = billService.getBillsForCustomer(subscriptionNumber);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).billId);
        assertEquals("2023-10", responses.get(0).billingPeriod);
        assertEquals("PENDING", responses.get(0).status);
        verify(billRepository, times(1)).findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber);
    }

    @Test
    void testGetBillEntityById_Success() {
        // Arrange
        when(billRepository.findById(1L)).thenReturn(Optional.of(mockBill));

        // Act
        Bill foundBill = billService.getBillEntityById(1L);

        // Assert
        assertNotNull(foundBill);
        assertEquals(1L, foundBill.getBillId());
        verify(billRepository, times(1)).findById(1L);
    }

    @Test
    void testGetBillEntityById_NotFound() {
        // Arrange
        when(billRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            billService.getBillEntityById(2L);
        });

        assertEquals("Bill not found: 2", exception.getMessage());
        verify(billRepository, times(1)).findById(2L);
    }
}
