package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.CustomerReportDTO;
import com.backend.water_management_system.dto.MonthlyReportDTO;
import com.backend.water_management_system.repository.UsageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerReportServiceTest {

    @Mock
    private UsageRecordRepository repository;

    private CustomerReportService service;

    @BeforeEach
    void setUp() {
        service = new CustomerReportService(repository);
    }

    @Test
    void getCustomerReport_withData_mapsToCustomerReportDTO() {
        // Arrange
        String customerId = "C001";
        int year = 2026;
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{"Jan", 50.5, 120.0});
        mockResults.add(new Object[]{"Feb", 60.0, 150.0});

        when(repository.getCustomerReport(customerId, year)).thenReturn(mockResults);

        // Act
        List<CustomerReportDTO> result = service.getCustomerReport(customerId, year);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Jan", result.get(0).getMonth());
        assertEquals(50.5, result.get(0).getTotalUsage());
        assertEquals(120.0, result.get(0).getTotalAmount());

        assertEquals("Feb", result.get(1).getMonth());
        assertEquals(60.0, result.get(1).getTotalUsage());
        assertEquals(150.0, result.get(1).getTotalAmount());

        verify(repository, times(1)).getCustomerReport(customerId, year);
    }

    @Test
    void getCustomerReport_emptyData_returnsEmptyList() {
        // Arrange
        String customerId = "C001";
        int year = 2026;
        when(repository.getCustomerReport(customerId, year)).thenReturn(Collections.emptyList());

        // Act
        List<CustomerReportDTO> result = service.getCustomerReport(customerId, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getMonthlyReport_withData_mapsToMonthlyReportDTO() {
        // Arrange
        int year = 2026;
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{"Jan", 5000.0, 25000.0});
        mockResults.add(new Object[]{"Feb", 6000.0, 30000.0});

        when(repository.getMonthlyReport(year)).thenReturn(mockResults);

        // Act
        List<MonthlyReportDTO> result = service.getMonthlyReport(year);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Jan", result.get(0).getMonth());
        assertEquals(5000.0, result.get(0).getUsage());
        assertEquals(25000.0, result.get(0).getRevenue());

        verify(repository, times(1)).getMonthlyReport(year);
    }
}
