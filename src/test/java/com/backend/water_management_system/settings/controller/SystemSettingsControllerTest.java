package com.backend.water_management_system.settings.controller;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.water_management_system.common.config.SecurityConfig;
import com.backend.water_management_system.security.CustomUserDetailsService;
import com.backend.water_management_system.security.JwtService;
import com.backend.water_management_system.settings.dto.SystemDetailsRequest;
import com.backend.water_management_system.settings.dto.SystemDetailsResponse;
import com.backend.water_management_system.settings.service.SystemSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(SystemSettingsController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "SUPER_ADMIN")
class SystemSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SystemSettingsService systemSettingsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void testGetSystemDetails_Success() throws Exception {
        SystemDetailsResponse response = SystemDetailsResponse.builder()
                .companyName("Water Management System")
                .officeEmail("admin@waterboard.lk")
                .overdueThreshold(new BigDecimal("5000.00"))
                .disconnectionGracePeriodDays(14)
                .reconnectionFee(new BigDecimal("1500.00"))
                .build();

        when(systemSettingsService.getSystemDetails()).thenReturn(response);

        mockMvc.perform(get("/api/system-settings/get"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Water Management System"))
                .andExpect(jsonPath("$.officeEmail").value("admin@waterboard.lk"))
                .andExpect(jsonPath("$.overdueThreshold").value(5000.00))
                .andExpect(jsonPath("$.disconnectionGracePeriodDays").value(14))
                .andExpect(jsonPath("$.reconnectionFee").value(1500.00));

        verify(systemSettingsService, times(1)).getSystemDetails();
    }

    @Test
    void testUpdateSystemDetails_Success() throws Exception {
        SystemDetailsRequest request = SystemDetailsRequest.builder()
                .companyName("Updated Water Board")
                .officeEmail("admin@waterboard.lk")
                .overdueThreshold(new BigDecimal("6000.00"))
                .disconnectionGracePeriodDays(20)
                .reconnectionFee(new BigDecimal("2500.00"))
                .build();

        SystemDetailsResponse response = SystemDetailsResponse.builder()
                .companyName("Updated Water Board")
                .officeEmail("admin@waterboard.lk")
                .overdueThreshold(new BigDecimal("6000.00"))
                .disconnectionGracePeriodDays(20)
                .reconnectionFee(new BigDecimal("2500.00"))
                .build();

        when(systemSettingsService.updateSystemDetails(any(SystemDetailsRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/system-settings/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Updated Water Board"))
                .andExpect(jsonPath("$.overdueThreshold").value(6000.00))
                .andExpect(jsonPath("$.disconnectionGracePeriodDays").value(20))
                .andExpect(jsonPath("$.reconnectionFee").value(2500.00));

        verify(systemSettingsService, times(1)).updateSystemDetails(any(SystemDetailsRequest.class));
    }

    @Test
    void testUpdateSystemDetails_NegativeOverdueThreshold_ValidationError() throws Exception {
        SystemDetailsRequest request = SystemDetailsRequest.builder()
                .overdueThreshold(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(put("/api/system-settings/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateSystemDetails_NegativeGracePeriod_ValidationError() throws Exception {
        SystemDetailsRequest request = SystemDetailsRequest.builder()
                .disconnectionGracePeriodDays(-5)
                .build();

        mockMvc.perform(put("/api/system-settings/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateSystemDetails_NegativeReconnectionFee_ValidationError() throws Exception {
        SystemDetailsRequest request = SystemDetailsRequest.builder()
                .reconnectionFee(new BigDecimal("-50.00"))
                .build();

        mockMvc.perform(put("/api/system-settings/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateSystemDetails_InvalidEmail_ValidationError() throws Exception {
        SystemDetailsRequest request = SystemDetailsRequest.builder()
                .officeEmail("not-a-valid-email")
                .build();

        mockMvc.perform(put("/api/system-settings/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
