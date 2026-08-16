package com.backend.water_management_system.controller;

import com.backend.water_management_system.config.SecurityConfig;
import com.backend.water_management_system.dto.MonthlyReportDTO;
import com.backend.water_management_system.service.CustomerReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

@WebMvcTest(MonthlyReportController.class)
@Import(SecurityConfig.class)
class MonthlyReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerReportService service;

    @Test
    void getMonthlyReport_returnsList() throws Exception {
        // Arrange
        int year = 2026;
        MonthlyReportDTO dto = new MonthlyReportDTO("Jan", 1000.0, 5000.0);

        when(service.getMonthlyReport(year)).thenReturn(List.of(dto));

        // Act & Assert
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
