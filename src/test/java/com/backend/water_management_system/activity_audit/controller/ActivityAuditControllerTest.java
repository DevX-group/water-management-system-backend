package com.backend.water_management_system.activity_audit.controller;

import com.backend.water_management_system.activity_audit.dto.ActivityAuditDetailResponse;
import com.backend.water_management_system.activity_audit.dto.ActivityAuditListResponse;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.exception.ActivityAuditLogNotFoundException;
import com.backend.water_management_system.activity_audit.service.ActivityAuditQueryService;
import com.backend.water_management_system.common.config.SecurityConfig;
import com.backend.water_management_system.common.dto.PaginationResponse;
import com.backend.water_management_system.security.CustomUserDetailsService;
import com.backend.water_management_system.security.JwtService;
import com.backend.water_management_system.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(ActivityAuditController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ActivityAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ActivityAuditQueryService queryService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void superAdminCanListWithDefaults() throws Exception {
        when(queryService.findAll(eq(0), eq(20), eq("occurredAt"), eq("desc"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(page());

        mockMvc.perform(get("/api/admin/activity-logs").with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.content[0].actorDisplayName").value("Administrator"))
                .andExpect(jsonPath("$.content[0].changedFields").doesNotExist());
    }

    @Test
    void systemAdminCanReadDetail() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryService.findById(id)).thenReturn(detail(id));

        mockMvc.perform(get("/api/admin/activity-logs/{id}", id)
                        .with(user("admin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.changedFields.email").value("changed"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("@"))));
    }

    @Test
    void detailNotFoundReturnsSafe404() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryService.findById(id)).thenThrow(new ActivityAuditLogNotFoundException(id));

        mockMvc.perform(get("/api/admin/activity-logs/{id}", id)
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACTIVITY_AUDIT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Activity audit log not found."));
    }

    @Test
    void entityHistoryEndpointIsAvailable() throws Exception {
        when(queryService.findEntityHistory(AuditEntityType.PAYMENT, "PAY-1", 0, 20)).thenReturn(page());
        mockMvc.perform(get("/api/admin/activity-logs/entity/PAYMENT/PAY-1")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidEnumsReturnSafeBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/activity-logs").param("action", "NOT_AN_ACTION")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter."));
    }

    @Test
    void invalidInstantReturnsSafeBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/activity-logs").param("from", "not-an-instant")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter."));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/activity-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerHandlerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/activity-logs").with(user("handler").roles("CUSTOMER_HANDLER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void meterReaderIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/activity-logs").with(user("reader").roles("METER_READER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/activity-logs").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noMutationMappingsExist() throws Exception {
        String path = "/api/admin/activity-logs";
        mockMvc.perform(post(path).with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put(path + "/" + UUID.randomUUID()).with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch(path + "/" + UUID.randomUUID()).with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete(path + "/" + UUID.randomUUID()).with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isMethodNotAllowed());
    }

    private PaginationResponse<ActivityAuditListResponse> page() {
        return PaginationResponse.<ActivityAuditListResponse>builder()
                .content(List.of(new ActivityAuditListResponse(
                        UUID.randomUUID(), Instant.parse("2026-08-23T10:00:00Z"), UUID.randomUUID(),
                        "Administrator", Role.SUPER_ADMIN, AuditAction.PAYMENT_CREATED,
                        AuditEntityType.PAYMENT, "PAY-1", "Payment PAY-1 was created.", AuditSource.WEB)))
                .currentPage(0).totalPages(1).totalElements(1).pageSize(20).last(true).build();
    }

    private ActivityAuditDetailResponse detail(UUID id) {
        return new ActivityAuditDetailResponse(
                id, Instant.parse("2026-08-23T10:00:00Z"), UUID.randomUUID(), "Administrator",
                Role.SYSTEM_ADMIN, AuditAction.USER_PROFILE_UPDATED, AuditEntityType.USER,
                UUID.randomUUID().toString(), "User account was updated.",
                Map.of("email", "changed"), AuditSource.WEB);
    }
}
