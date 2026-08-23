package com.backend.water_management_system.reports.controller;

import com.backend.water_management_system.common.config.SecurityConfig;
import com.backend.water_management_system.reports.dto.MonthlyReportDTO;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonthlyReportController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class MonthlyReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerReportService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
        @WithMockUser(roles = "SUPER_ADMIN")
    void getMonthlyReport_returnsList() throws Exception {
        int year = 2026;

        MonthlyReportDTO dto =
                new MonthlyReportDTO("Jan", 1000.0, 5000.0);

        when(service.getMonthlyReport(year))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/reports/monthly")
                        .param("year", String.valueOf(year))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].month", is("Jan")))
                .andExpect(jsonPath("$[0].usage", is(1000.0)))
                .andExpect(jsonPath("$[0].revenue", is(5000.0)));

        verify(service, times(1)).getMonthlyReport(year);
    }
}
