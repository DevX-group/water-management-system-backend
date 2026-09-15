package com.backend.water_management_system.settings.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.settings.dto.SystemDetailsRequest;
import com.backend.water_management_system.settings.dto.SystemDetailsResponse;
import com.backend.water_management_system.settings.entity.SystemDetails;
import com.backend.water_management_system.settings.repository.SystemDetailsRepository;

@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceTest {

    @Mock
    private SystemDetailsRepository systemDetailsRepository;

    @InjectMocks
    private SystemSettingsService systemSettingsService;

    private SystemDetails existingDetails;

    @BeforeEach
    void setUp() {
        existingDetails = SystemDetails.builder()
                .id(1L)
                .companyName("Water Board")
                .officeAddress("Colombo 01")
                .officeContactNumber("0112233445")
                .officeEmail("info@waterboard.lk")
                .defaultCurrency("LKR")
                .bankName("Bank of Ceylon")
                .branch("Fort")
                .accountNumber("12345678")
                .accountName("Water Board Main")
                .overdueThreshold(new BigDecimal("5000.00"))
                .disconnectionGracePeriodDays(14)
                .reconnectionFee(new BigDecimal("1500.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetSystemDetails_Success() {
        when(systemDetailsRepository.findById(1L)).thenReturn(Optional.of(existingDetails));

        SystemDetailsResponse response = systemSettingsService.getSystemDetails();

        assertNotNull(response);
        assertEquals("Water Board", response.getCompanyName());
        assertEquals("Colombo 01", response.getOfficeAddress());
        assertEquals("info@waterboard.lk", response.getOfficeEmail());
        assertEquals(new BigDecimal("5000.00"), response.getOverdueThreshold());
        assertEquals(14, response.getDisconnectionGracePeriodDays());
        assertEquals(new BigDecimal("1500.00"), response.getReconnectionFee());
    }

    @Test
    void testGetSystemDetails_WhenNotInitialized_ThrowsException() {
        when(systemDetailsRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            systemSettingsService.getSystemDetails();
        });

        assertEquals("System details not initialized", exception.getMessage());
    }

    @Test
    void testUpdateSystemDetails_UpdatesAllFields() {
        when(systemDetailsRepository.findById(1L)).thenReturn(Optional.of(existingDetails));
        when(systemDetailsRepository.save(any(SystemDetails.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemDetailsRequest request = SystemDetailsRequest.builder()
                .companyName("Updated Water Board")
                .officeAddress("Kandy 02")
                .officeContactNumber("0812345678")
                .officeEmail("admin@waterboard.lk")
                .defaultCurrency("USD")
                .bankName("Peoples Bank")
                .branch("Kandy Main")
                .accountNumber("87654321")
                .accountName("Water Board Kandy")
                .overdueThreshold(new BigDecimal("7500.00"))
                .disconnectionGracePeriodDays(21)
                .reconnectionFee(new BigDecimal("2000.00"))
                .build();

        SystemDetailsResponse response = systemSettingsService.updateSystemDetails(request);

        assertNotNull(response);
        assertEquals("Updated Water Board", response.getCompanyName());
        assertEquals("Kandy 02", response.getOfficeAddress());
        assertEquals("admin@waterboard.lk", response.getOfficeEmail());
        assertEquals(new BigDecimal("7500.00"), response.getOverdueThreshold());
        assertEquals(21, response.getDisconnectionGracePeriodDays());
        assertEquals(new BigDecimal("2000.00"), response.getReconnectionFee());

        verify(systemDetailsRepository).save(any(SystemDetails.class));
    }

    @Test
    void testUpdateSystemDetails_PartialUpdateLeavesUntouched() {
        when(systemDetailsRepository.findById(1L)).thenReturn(Optional.of(existingDetails));
        when(systemDetailsRepository.save(any(SystemDetails.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemDetailsRequest request = SystemDetailsRequest.builder()
                .overdueThreshold(new BigDecimal("10000.00"))
                .disconnectionGracePeriodDays(30)
                .reconnectionFee(new BigDecimal("2500.00"))
                .build();

        SystemDetailsResponse response = systemSettingsService.updateSystemDetails(request);

        assertNotNull(response);
        assertEquals("Water Board", response.getCompanyName());
        assertEquals(new BigDecimal("10000.00"), response.getOverdueThreshold());
        assertEquals(30, response.getDisconnectionGracePeriodDays());
        assertEquals(new BigDecimal("2500.00"), response.getReconnectionFee());
    }
}
