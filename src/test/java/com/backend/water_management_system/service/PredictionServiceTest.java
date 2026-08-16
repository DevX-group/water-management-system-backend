package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.CustomerPredictionResponse;
import com.backend.water_management_system.dto.MonthlyPredictionResponse;
import com.backend.water_management_system.repository.UsageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private UsageRecordRepository repository;

    @Mock
    private RestTemplate mockRestTemplate;

    private PredictionService service;

    @BeforeEach
    void setUp() {
        service = new PredictionService(repository);
        // Inject the mocked RestTemplate
        ReflectionTestUtils.setField(service, "restTemplate", mockRestTemplate);
    }

    @Test
    void getMonthlyPrediction_success() {
        // Arrange
        int year = 2026;
        List<Object[]> mockReport = new ArrayList<>();
        mockReport.add(new Object[]{"Jan", 100.0});
        mockReport.add(new Object[]{"Feb", 120.0});

        when(repository.getMonthlyReport(year)).thenReturn(mockReport);

        String mockFlaskResponse = "[{\"yhat\": 130.0}, {\"yhat\": 140.0}]";
        when(mockRestTemplate.postForObject(
                eq("http://127.0.0.1:5000/predict"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockFlaskResponse);

        // Act
        List<MonthlyPredictionResponse> responses = service.getMonthlyPrediction(year);

        // Assert
        assertNotNull(responses);
        // Expected total responses = 2 actual data months + 2 predicted months = 4
        assertEquals(4, responses.size());

        // First 2 elements should have actual usage
        assertEquals("Jan", responses.get(0).getMonth());
        assertEquals(100.0, responses.get(0).getUsage());
        assertNull(responses.get(0).getPredictedUsage());

        assertEquals("Feb", responses.get(1).getMonth());
        assertEquals(120.0, responses.get(1).getUsage());
        assertNull(responses.get(1).getPredictedUsage());

        // Next 2 elements should have predicted usage (sequentially generated months starting from March)
        assertEquals("Mar", responses.get(2).getMonth());
        assertNull(responses.get(2).getUsage());
        assertEquals(130.0, responses.get(2).getPredictedUsage());

        assertEquals("Apr", responses.get(3).getMonth());
        assertNull(responses.get(3).getUsage());
        assertEquals(140.0, responses.get(3).getPredictedUsage());

        verify(repository, times(1)).getMonthlyReport(year);
        verify(mockRestTemplate, times(1)).postForObject(anyString(), any(), any());
    }

    @Test
    void getCustomerPrediction_success() {
        // Arrange
        String customerId = "C001";
        int year = 2026;
        List<Object[]> mockReport = new ArrayList<>();
        mockReport.add(new Object[]{"Jan", 10.0});
        mockReport.add(new Object[]{"Feb", 12.0});

        when(repository.getCustomerPredictionData(customerId, year)).thenReturn(mockReport);

        String mockFlaskResponse = "[{\"yhat\": 14.0}, {\"yhat\": 15.0}]";
        when(mockRestTemplate.postForObject(
                eq("http://127.0.0.1:5000/predict"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockFlaskResponse);

        // Act
        List<CustomerPredictionResponse> responses = service.getCustomerPrediction(customerId, year);

        // Assert
        assertNotNull(responses);
        assertEquals(4, responses.size());

        // First 2: actual data
        assertEquals("Jan", responses.get(0).getMonth());
        assertEquals(10.0, responses.get(0).getUsage());
        assertNull(responses.get(0).getPredictedUsage());

        // Next 2: predictions
        assertEquals("Mar", responses.get(2).getMonth());
        assertNull(responses.get(2).getUsage());
        assertEquals(14.0, responses.get(2).getPredictedUsage());

        verify(repository, times(1)).getCustomerPredictionData(customerId, year);
        verify(mockRestTemplate, times(1)).postForObject(anyString(), any(), any());
    }
}
