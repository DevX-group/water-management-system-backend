package com.backend.water_management_system.reports.controller;

import com.backend.water_management_system.common.config.SecurityConfig;
import com.backend.water_management_system.reports.dto.CustomerReportDTO;
import com.backend.water_management_system.reports.service.CustomerReportService;
import com.backend.water_management_system.security.CustomUserDetailsService;
import com.backend.water_management_system.security.JwtService;

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

@WebMvcTest(CustomerReportController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerReportService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getCustomerReport_returnsList() throws Exception {
        String customerId = "C001";
        int year = 2026;

        CustomerReportDTO dto =
                new CustomerReportDTO("Jan", 50.0, 150.0);

        when(service.getCustomerReport(customerId, year))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/reports/customer")
                        .param("customerId", customerId)
                        .param("year", String.valueOf(year))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].month", is("Jan")))
                .andExpect(jsonPath("$[0].totalUsage", is(50.0)))
                .andExpect(jsonPath("$[0].totalAmount", is(150.0)));

        verify(service, times(1))
                .getCustomerReport(customerId, year);
    }
}
