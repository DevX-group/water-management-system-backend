package com.backend.water_management_system.auth.controller;

import com.backend.water_management_system.auth.dto.PasswordResetRequest;
import com.backend.water_management_system.auth.exception.PasswordResetRateLimitException;
import com.backend.water_management_system.auth.service.PasswordResetService;
import com.backend.water_management_system.common.config.SecurityConfig;
import com.backend.water_management_system.security.CustomUserDetailsService;
import com.backend.water_management_system.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class PasswordResetControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private CustomUserDetailsService customUserDetailsService;

    @Test
    void exactPasswordResetRoutesArePublicAndReturnServiceResponse() throws Exception {
        when(passwordResetService.request(any(PasswordResetRequest.class), anyString()))
                .thenReturn(Map.of("message", PasswordResetService.REQUEST_MESSAGE));

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nic\":\"200012345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetService.REQUEST_MESSAGE));
    }

    @Test
    void invalidRequestUsesValidationErrorConvention() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nic\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void rateLimitReturnsGeneric429AndRetryAfter() throws Exception {
        when(passwordResetService.request(any(PasswordResetRequest.class), anyString()))
                .thenThrow(new PasswordResetRateLimitException());

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nic\":\"200012345678\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }

    @Test
    void unrelatedAuthRouteIsNotPublic() throws Exception {
        mockMvc.perform(post("/api/auth/not-public")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                        .andExpect(status().isForbidden());
    }
}
