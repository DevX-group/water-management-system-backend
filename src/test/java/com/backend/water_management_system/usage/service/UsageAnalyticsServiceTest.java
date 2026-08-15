package com.backend.water_management_system.usage.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.meter_reading.entity.MeterReading;
import com.backend.water_management_system.meter_reading.repository.MeterReadingRepository;
import com.backend.water_management_system.usage.dto.UsageAnalyticsResponse;

@ExtendWith(MockitoExtension.class)
public class UsageAnalyticsServiceTest {

    @Mock
    private MeterReadingRepository meterReadingRepository;

    @InjectMocks
    private UsageAnalyticsService usageAnalyticsService;

    private MeterReading reading1;
    private MeterReading reading2;
    private Customer mockCustomer;

    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setConnectionType("domestic");
        
        reading1 = new MeterReading();
        reading1.setReadingDate(LocalDate.of(2023, 1, 15));
        reading1.setUsageUnits(100);
        reading1.setCustomer(mockCustomer);

        reading2 = new MeterReading();
        reading2.setReadingDate(LocalDate.of(2023, 2, 10));
        reading2.setUsageUnits(200);
        reading2.setCustomer(mockCustomer);
    }

    @Test
    void testGetAnalytics_SystemWide() {
        // Arrange
        when(meterReadingRepository.findAllByYear(2023)).thenReturn(List.of(reading1, reading2));

        // Act
        UsageAnalyticsResponse response = usageAnalyticsService.getAnalytics(2023);

        // Assert
        assertNotNull(response);
        assertEquals(300, response.totalUsage);
        assertEquals(200, response.peakUsage);
        assertEquals(0, response.minimumUsage); // 0 because other 10 months have 0 usage
        assertEquals(300 / 12, response.averageUsage);
        
        // Month 1 (Jan) should have 100
        assertEquals(100, response.monthlyData.get(0).usage);
        // Month 2 (Feb) should have 200
        assertEquals(200, response.monthlyData.get(1).usage);
        
        verify(meterReadingRepository, times(1)).findAllByYear(2023);
    }

    @Test
    void testGetAnalytics_PerCustomer() {
        // Arrange
        String subscriptionNumber = "SUB123";
        when(meterReadingRepository.findByCustomerAndYear(subscriptionNumber, 2023))
                .thenReturn(List.of(reading1));

        // Act
        UsageAnalyticsResponse response = usageAnalyticsService.getAnalytics(subscriptionNumber, 2023);

        // Assert
        assertNotNull(response);
        assertEquals(100, response.totalUsage);
        assertEquals(100, response.peakUsage);
        assertEquals(100, response.monthlyData.get(0).usage);
        assertEquals(0, response.monthlyData.get(1).usage);
        
        verify(meterReadingRepository, times(1)).findByCustomerAndYear(subscriptionNumber, 2023);
    }
}
