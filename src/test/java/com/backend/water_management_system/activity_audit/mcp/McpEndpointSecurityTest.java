package com.backend.water_management_system.activity_audit.mcp;

import com.backend.water_management_system.activity_audit.controller.ActivityAuditController;
import com.backend.water_management_system.activity_audit.service.ActivityAuditQueryService;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityAuditController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class McpEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private ActivityAuditQueryService activityAuditQueryService;

    @Test
    void anonymousMcpRequestIsRejected() throws Exception {
        mockMvc.perform(post("/mcp"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminMcpRequestIsForbidden() throws Exception {
        mockMvc.perform(post("/mcp").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }
}
