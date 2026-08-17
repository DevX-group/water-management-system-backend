package com.backend.water_management_system.controller;

import com.backend.water_management_system.common.config.SecurityConfig;
import com.backend.water_management_system.dto.AreaReportDTO;
import com.backend.water_management_system.security.CustomUserDetailsService;
import com.backend.water_management_system.security.JwtService;
import com.backend.water_management_system.service.AreaReportService;

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

@WebMvcTest(AreaReportController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AreaReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AreaReportService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAreaReport_returnsJsonList() throws Exception {
        int year = 2026;

        AreaReportDTO dto = new AreaReportDTO();
        dto.setMonth("Jan");
        dto.setArea1Usage(100.0);
        dto.setArea1Revenue(500.0);

        when(service.getAreaReport(year)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/reports/area")
                        .param("year", String.valueOf(year))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].month", is("Jan")))
                .andExpect(jsonPath("$[0].area1Usage", is(100.0)))
                .andExpect(jsonPath("$[0].area1Revenue", is(500.0)));

        verify(service, times(1)).getAreaReport(year);
    }
}