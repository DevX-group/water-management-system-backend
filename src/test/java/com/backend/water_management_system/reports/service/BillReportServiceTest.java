package com.backend.water_management_system.reports.service;

import com.backend.water_management_system.reports.dto.BillsSummaryDTO;
import com.backend.water_management_system.reports.entity.BillReport;
import com.backend.water_management_system.reports.repository.BillReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillReportServiceTest {

    @Mock
    private BillReportRepository repository;

    private BillReportService service;

    @BeforeEach
    void setUp() {
        service = new BillReportService(repository);
    }

    @Test
    void getBills_returnsList() {
        // Arrange
        List<BillReport> mockBills = List.of(
                new BillReport("1", "C001", "John Doe", 500.0, LocalDate.now().plusDays(5), LocalDate.now().minusDays(5), "UNPAID"),
                new BillReport("2", "C002", "Jane Doe", 600.0, LocalDate.now().plusDays(6), LocalDate.now().minusDays(4), "PAID")
        );
        when(repository.findFilteredBills("")).thenReturn(mockBills);

        // Act
        List<BillReport> result = service.getBills(null);

        // Assert
        assertEquals(2, result.size());
        verify(repository, times(1)).findFilteredBills("");
    }

    @Test
    void getBillsByCustomer_returnsFilteredList() {
        // Arrange
        String customerId = "C001";
        List<BillReport> mockBills = List.of(
                new BillReport("1", customerId, "John Doe", 500.0, LocalDate.now().plusDays(5), LocalDate.now().minusDays(5), "UNPAID")
        );
        when(repository.findByCustomerId(customerId)).thenReturn(mockBills);

        // Act
        List<BillReport> result = service.getBillsByCustomer(customerId);

        // Assert
        assertEquals(1, result.size());
        assertEquals(customerId, result.get(0).getCustomerId());
        verify(repository, times(1)).findByCustomerId(customerId);
    }

    @Test
    void getOverdueBills_returnsUnpaidAndDueDateBeforeToday() {
        // Arrange
        LocalDate today = LocalDate.now();
        List<BillReport> mockOverdueBills = List.of(
                // Unpaid & Overdue (Due 5 days ago)
                new BillReport("1", "C001", "John Doe", 500.0, today.minusDays(5), today.minusDays(15), "UNPAID")
        );
        when(repository.findOverdueBills(any(LocalDate.class), eq(""))).thenReturn(mockOverdueBills);

        // Act
        List<BillReport> result = service.getOverdueBills(null);

        // Assert
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
        verify(repository, times(1)).findOverdueBills(any(LocalDate.class), eq(""));
    }

    @Test
    void getSummary_returnsCorrectCalculations() {
        // Arrange
        String customerId = "C001";
        LocalDate today = LocalDate.now();
        List<BillReport> mockBills = List.of(
                new BillReport("1", customerId, "John Doe", 300.0, today.plusDays(5), today.minusDays(10), "PAID"),
                new BillReport("2", customerId, "John Doe", 500.0, today.minusDays(2), today.minusDays(20), "UNPAID"),
                new BillReport("3", customerId, "John Doe", 200.0, today.plusDays(20), today.plusDays(5), "UNPAID")
        );
        when(repository.findByCustomerId(customerId)).thenReturn(mockBills);

        // Act
        BillsSummaryDTO summary = service.getSummary(customerId);

        // Assert
        assertNotNull(summary);
        assertEquals(customerId, summary.getCustomerId());
        assertEquals("John Doe", summary.getCustomerName());
        assertEquals(1000.0, summary.getTotalAmount()); // 300 + 500 + 200
        assertEquals(2, summary.getUnpaidCount()); // Bill 2 and 3
        assertEquals(today.plusDays(5), summary.getLastBillDate()); // Bill 3 has the latest bill report date (today + 5)
        verify(repository, times(1)).findByCustomerId(customerId);
    }

    @Test
    void getSummary_emptyBills_returnsDefaultSummary() {
        // Arrange
        String customerId = "C001";
        when(repository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        // Act
        BillsSummaryDTO summary = service.getSummary(customerId);

        // Assert
        assertNotNull(summary);
        assertEquals(customerId, summary.getCustomerId());
        assertNull(summary.getCustomerName());
        assertEquals(0.0, summary.getTotalAmount());
        assertEquals(0, summary.getUnpaidCount());
        assertNull(summary.getLastBillDate());
    }
}
