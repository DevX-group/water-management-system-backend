package com.backend.water_management_system.activity_audit.mcp;

import com.backend.water_management_system.activity_audit.dto.ActivityAuditDetailResponse;
import com.backend.water_management_system.activity_audit.dto.ActivityAuditListResponse;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.enums.AuditSource;
import com.backend.water_management_system.activity_audit.service.ActivityAuditQueryService;
import com.backend.water_management_system.common.dto.PaginationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.mcp.annotation.McpTool;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAuditMcpToolsTest {

    @Mock
    private ActivityAuditQueryService queryService;

    private ActivityAuditMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new ActivityAuditMcpTools(queryService);
    }

    @Test
    void searchUsesSafeDefaults() {
        PaginationResponse<ActivityAuditListResponse> expected = emptyPage();
        when(queryService.findAll(0, 20, "occurredAt", "desc",
                null, null, null, null, null, null, null)).thenReturn(expected);

        PaginationResponse<McpActivityAuditListResponse> actual = tools.searchActivityLogs(
                null, null, null, null, null, null, null, null, null, null, null);

        assertPageMetadata(expected, actual);
    }

    @Test
    void searchParsesSupportedFilters() {
        Instant from = Instant.parse("2026-08-20T00:00:00Z");
        Instant to = Instant.parse("2026-08-23T23:59:59Z");
        UUID actorId = UUID.randomUUID();
        PaginationResponse<ActivityAuditListResponse> expected = emptyPage();
        when(queryService.findAll(1, 10, "action", "asc", from, to,
                AuditAction.PAYMENT_CREATED, AuditEntityType.PAYMENT,
                actorId, "Administrator", AuditSource.WEB)).thenReturn(expected);

        PaginationResponse<McpActivityAuditListResponse> actual = tools.searchActivityLogs(
                1, 10, "action", "asc", from.toString(), to.toString(),
                "payment_created", "payment", actorId.toString(), " Administrator ", "web");

        assertPageMetadata(expected, actual);
    }

    @Test
    void detailDelegatesUsingUuid() {
        UUID id = UUID.randomUUID();
        ActivityAuditDetailResponse expected = new ActivityAuditDetailResponse(
                id, Instant.parse("2026-08-23T10:00:00Z"), UUID.randomUUID(), "Administrator",
                null, AuditAction.PAYMENT_CREATED, AuditEntityType.PAYMENT,
                "PAY-1", "Payment was created.", Map.of(), AuditSource.WEB);
        when(queryService.findById(id)).thenReturn(expected);

        assertEquals(McpActivityAuditDetailResponse.from(expected), tools.getActivityLog(id.toString()));
        verify(queryService).findById(id);
    }

    @Test
    void entityHistoryDelegatesWithDefaults() {
        PaginationResponse<ActivityAuditListResponse> expected = emptyPage();
        when(queryService.findEntityHistory(AuditEntityType.PAYMENT, "PAY-1", 0, 20))
                .thenReturn(expected);

        assertPageMetadata(expected, tools.getEntityActivityHistory("payment", " PAY-1 ", null, null));
    }

    @Test
    void systemActorNullFieldsAreOmittedFromMcpJson() throws JsonProcessingException {
        McpActivityAuditListResponse response = McpActivityAuditListResponse.from(
                new ActivityAuditListResponse(
                        UUID.randomUUID(),
                        null,
                        null,
                        "System",
                        null,
                        AuditAction.PAYMENT_CREATED,
                        AuditEntityType.PAYMENT,
                        "PAY-1",
                        "System event.",
                        AuditSource.SYSTEM));

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);

        assertFalse(json.contains("actorUserId"));
        assertFalse(json.contains("actorRole"));
        assertTrue(json.contains("\"actorDisplayName\":\"System\""));
    }

    @Test
    void invalidInputsAreRejectedBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> tools.getActivityLog("not-a-uuid"));
        assertThrows(IllegalArgumentException.class, () -> tools.searchActivityLogs(
                null, null, null, null, "yesterday", null,
                null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> tools.getEntityActivityHistory(
                "unknown", "1", null, null));
    }

    @Test
    void allToolsDeclareReadOnlyClosedWorldHints() {
        List<McpTool> annotations = List.of(
                annotation("searchActivityLogs", Integer.class, Integer.class, String.class, String.class,
                        String.class, String.class, String.class, String.class, String.class, String.class, String.class),
                annotation("getActivityLog", String.class),
                annotation("getEntityActivityHistory", String.class, String.class, Integer.class, Integer.class));

        assertEquals(List.of("search_activity_logs", "get_activity_log", "get_entity_activity_history"),
                annotations.stream().map(McpTool::name).toList());
        annotations.forEach(annotation -> {
            assertTrue(annotation.annotations().readOnlyHint());
            assertFalse(annotation.annotations().destructiveHint());
            assertTrue(annotation.annotations().idempotentHint());
            assertFalse(annotation.annotations().openWorldHint());
            assertFalse(annotation.generateOutputSchema());
        });
    }

    private McpTool annotation(String methodName, Class<?>... parameterTypes) {
        try {
            Method method = ActivityAuditMcpTools.class.getMethod(methodName, parameterTypes);
            return method.getAnnotation(McpTool.class);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    private PaginationResponse<ActivityAuditListResponse> emptyPage() {
        return PaginationResponse.<ActivityAuditListResponse>builder()
                .content(List.of())
                .currentPage(0)
                .totalPages(0)
                .totalElements(0)
                .pageSize(20)
                .last(true)
                .build();
    }

    private void assertPageMetadata(
            PaginationResponse<ActivityAuditListResponse> expected,
            PaginationResponse<McpActivityAuditListResponse> actual) {
        assertEquals(expected.getCurrentPage(), actual.getCurrentPage());
        assertEquals(expected.getTotalPages(), actual.getTotalPages());
        assertEquals(expected.getTotalElements(), actual.getTotalElements());
        assertEquals(expected.getPageSize(), actual.getPageSize());
        assertEquals(expected.isLast(), actual.isLast());
        assertEquals(expected.getContent().size(), actual.getContent().size());
    }
}
