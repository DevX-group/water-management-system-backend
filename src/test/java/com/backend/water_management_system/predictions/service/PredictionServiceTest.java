package com.backend.water_management_system.predictions.service;

import com.backend.water_management_system.predictions.dto.CustomerPredictionResponse;
import com.backend.water_management_system.predictions.dto.MonthlyPredictionResponse;
import com.backend.water_management_system.reports.repository.UsageRecordRepository;
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

    private static final String FLASK_URL =
            "http://127.0.0.1:5000/predict";

    @Mock
    private UsageRecordRepository repository;

    @Mock
    private RestTemplate mockRestTemplate;

    private PredictionService service;

    @BeforeEach
    void setUp() {
        service = new PredictionService(repository);

        ReflectionTestUtils.setField(
                service,
                "restTemplate",
                mockRestTemplate
        );
    }

    @Test
    void getMonthlyPrediction_success() {
        // Arrange
        int year = 2026;

        List<Object[]> mockReport =
                new ArrayList<>();

        /*
         * New repository row structure:
         * row[0] = month
         * row[1] = month number
         * row[2] = usage
         * row[3] = revenue
         */
        mockReport.add(
                new Object[]{
                        "Jan",
                        1,
                        100.0,
                        1500.0
                }
        );

        mockReport.add(
                new Object[]{
                        "Feb",
                        2,
                        120.0,
                        1800.0
                }
        );

        when(
                repository.getMonthlyPredictionData(year)
        ).thenReturn(mockReport);

        String usagePredictionResponse = """
                [
                    {"yhat": 130.0},
                    {"yhat": 140.0}
                ]
                """;

        String revenuePredictionResponse = """
                [
                    {"yhat": 1950.0},
                    {"yhat": 2100.0}
                ]
                """;

        /*
         * PredictionService calls Flask twice:
         * 1. Usage prediction
         * 2. Revenue prediction
         */
        when(
                mockRestTemplate.postForObject(
                        eq(FLASK_URL),
                        any(HttpEntity.class),
                        eq(String.class)
                )
        ).thenReturn(
                usagePredictionResponse,
                revenuePredictionResponse
        );

        // Act
        List<MonthlyPredictionResponse> responses =
                service.getMonthlyPrediction(year);

        // Assert
        assertNotNull(responses);
        assertEquals(4, responses.size());

        // January actual values
        MonthlyPredictionResponse january =
                responses.get(0);

        assertEquals("Jan", january.getMonth());
        assertEquals(100.0, january.getUsage());
        assertEquals(1500.0, january.getRevenue());
        assertNull(january.getPredictedUsage());
        assertNull(january.getPredictedRevenue());

        // February actual values
        MonthlyPredictionResponse february =
                responses.get(1);

        assertEquals("Feb", february.getMonth());
        assertEquals(120.0, february.getUsage());
        assertEquals(1800.0, february.getRevenue());
        assertNull(february.getPredictedUsage());
        assertNull(february.getPredictedRevenue());

        // March predicted values
        MonthlyPredictionResponse march =
                responses.get(2);

        assertEquals("Mar", march.getMonth());
        assertNull(march.getUsage());
        assertNull(march.getRevenue());
        assertEquals(
                130.0,
                march.getPredictedUsage()
        );
        assertEquals(
                1950.0,
                march.getPredictedRevenue()
        );

        // April predicted values
        MonthlyPredictionResponse april =
                responses.get(3);

        assertEquals("Apr", april.getMonth());
        assertNull(april.getUsage());
        assertNull(april.getRevenue());
        assertEquals(
                140.0,
                april.getPredictedUsage()
        );
        assertEquals(
                2100.0,
                april.getPredictedRevenue()
        );

        verify(repository, times(1))
                .getMonthlyPredictionData(year);

        verify(mockRestTemplate, times(2))
                .postForObject(
                        eq(FLASK_URL),
                        any(HttpEntity.class),
                        eq(String.class)
                );
    }

    @Test
    void getCustomerPrediction_success() {
        // Arrange
        String customerId = "C001";
        int year = 2026;

        List<Object[]> mockReport =
                new ArrayList<>();

        mockReport.add(
                new Object[]{
                        "Jan",
                        1,
                        10.0,
                        150.0
                }
        );

        mockReport.add(
                new Object[]{
                        "Feb",
                        2,
                        12.0,
                        180.0
                }
        );

        when(
                repository.getCustomerPredictionData(
                        customerId,
                        year
                )
        ).thenReturn(mockReport);

        String usagePredictionResponse = """
                [
                    {"yhat": 14.0},
                    {"yhat": 15.0}
                ]
                """;

        String revenuePredictionResponse = """
                [
                    {"yhat": 210.0},
                    {"yhat": 225.0}
                ]
                """;

        when(
                mockRestTemplate.postForObject(
                        eq(FLASK_URL),
                        any(HttpEntity.class),
                        eq(String.class)
                )
        ).thenReturn(
                usagePredictionResponse,
                revenuePredictionResponse
        );

        // Act
        List<CustomerPredictionResponse> responses =
                service.getCustomerPrediction(
                        customerId,
                        year
                );

        // Assert
        assertNotNull(responses);
        assertEquals(4, responses.size());

        // January actual values
        CustomerPredictionResponse january =
                responses.get(0);

        assertEquals("Jan", january.getMonth());
        assertEquals(10.0, january.getUsage());
        assertEquals(150.0, january.getRevenue());
        assertNull(january.getPredictedUsage());
        assertNull(january.getPredictedRevenue());

        // February actual values
        CustomerPredictionResponse february =
                responses.get(1);

        assertEquals("Feb", february.getMonth());
        assertEquals(12.0, february.getUsage());
        assertEquals(180.0, february.getRevenue());

        // March predicted values
        CustomerPredictionResponse march =
                responses.get(2);

        assertEquals("Mar", march.getMonth());
        assertNull(march.getUsage());
        assertNull(march.getRevenue());
        assertEquals(
                14.0,
                march.getPredictedUsage()
        );
        assertEquals(
                210.0,
                march.getPredictedRevenue()
        );

        // April predicted values
        CustomerPredictionResponse april =
                responses.get(3);

        assertEquals("Apr", april.getMonth());
        assertNull(april.getUsage());
        assertNull(april.getRevenue());
        assertEquals(
                15.0,
                april.getPredictedUsage()
        );
        assertEquals(
                225.0,
                april.getPredictedRevenue()
        );

        verify(repository, times(1))
                .getCustomerPredictionData(
                        customerId,
                        year
                );

        verify(mockRestTemplate, times(2))
                .postForObject(
                        eq(FLASK_URL),
                        any(HttpEntity.class),
                        eq(String.class)
                );
    }
}