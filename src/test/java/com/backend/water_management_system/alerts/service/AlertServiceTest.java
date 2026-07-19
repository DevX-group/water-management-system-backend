package com.backend.water_management_system.alerts.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.alerts.entity.Alert;
import com.backend.water_management_system.alerts.repository.AlertRepository;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertService alertService;

    private Alert alert1;
    private Alert alert2;

    @BeforeEach
    void setUp() {
        alert1 = Alert.builder()
                .id(1L)
                .severity("high")
                .title("Leak Detected")
                .description("Possible leak.")
                .time(LocalDateTime.now())
                .dismissed(false)
                .build();

        alert2 = Alert.builder()
                .id(2L)
                .severity("medium")
                .title("Usage Spike")
                .description("High usage.")
                .time(LocalDateTime.now())
                .dismissed(false)
                .build();
    }

    @Test
    void testGetActiveAlerts_WithSeverity() {
        // Arrange
        when(alertRepository.findBySeverityAndDismissedFalse("high")).thenReturn(List.of(alert1));

        // Act
        List<Alert> alerts = alertService.getActiveAlerts("high");

        // Assert
        assertEquals(1, alerts.size());
        assertEquals("high", alerts.get(0).getSeverity());
        verify(alertRepository, times(1)).findBySeverityAndDismissedFalse("high");
        verify(alertRepository, never()).findByDismissedFalseOrderByTimeDesc();
    }

    @Test
    void testGetActiveAlerts_All() {
        // Arrange
        when(alertRepository.findByDismissedFalseOrderByTimeDesc()).thenReturn(List.of(alert1, alert2));

        // Act
        List<Alert> alerts = alertService.getActiveAlerts("all");

        // Assert
        assertEquals(2, alerts.size());
        verify(alertRepository, times(1)).findByDismissedFalseOrderByTimeDesc();
    }

    @Test
    void testGetSeverityCounts() {
        // Arrange
        when(alertRepository.findByDismissedFalseOrderByTimeDesc()).thenReturn(List.of(alert1, alert2, alert1));

        // Act
        Map<String, Long> counts = alertService.getSeverityCounts();

        // Assert
        assertEquals(2L, counts.get("high"));
        assertEquals(1L, counts.get("medium"));
    }

    @Test
    void testDismissAlert() {
        // Arrange
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert1));

        // Act
        alertService.dismissAlert(1L);

        // Assert
        assertTrue(alert1.isDismissed());
        verify(alertRepository, times(1)).save(alert1);
    }

    @Test
    void testCreateAlert() {
        // Arrange
        when(alertRepository.save(any(Alert.class))).thenReturn(alert1);

        // Act
        alertService.createAlert("high", "Title", "Desc", "1000", "SUB-001");

        // Assert
        verify(alertRepository, times(1)).save(any(Alert.class));
    }
}
