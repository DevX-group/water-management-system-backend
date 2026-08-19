package com.backend.water_management_system.controller;

import com.backend.water_management_system.common.config.SecurityConfig;
import com.backend.water_management_system.dto.CustomerPredictionResponse;
import com.backend.water_management_system.dto.MonthlyPredictionResponse;
import com.backend.water_management_system.security.CustomUserDetailsService;
import com.backend.water_management_system.security.JwtService;
import com.backend.water_management_system.service.PredictionService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PredictionController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class PredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PredictionService predictionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void monthlyPrediction_returnsList() throws Exception {
        int year = 2026;

        MonthlyPredictionResponse response =
                new MonthlyPredictionResponse("Jan", 100.0, null);

        when(predictionService.getMonthlyPrediction(year))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/predictions/monthly")
                        .param("year", String.valueOf(year))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].month", is("Jan")))
                .andExpect(jsonPath("$[0].usage", is(100.0)));

        verify(predictionService, times(1))
                .getMonthlyPrediction(year);
    }

    @Test
    void customerPrediction_returnsList() throws Exception {
        String customerId = "C001";
        int year = 2026;

        CustomerPredictionResponse response =
                new CustomerPredictionResponse("Jan", 10.0, null);

        when(predictionService.getCustomerPrediction(customerId, year))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/predictions/customer")
                        .param("customerId", customerId)
                        .param("year", String.valueOf(year))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].month", is("Jan")))
                .andExpect(jsonPath("$[0].usage", is(10.0)));

        verify(predictionService, times(1))
                .getCustomerPrediction(customerId, year);
    }
}