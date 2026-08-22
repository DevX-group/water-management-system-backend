package com.backend.water_management_system.dashboard.controller;

import com.backend.water_management_system.dashboard.dto.DashboardWidgetDTO;
import com.backend.water_management_system.dashboard.dto.WidgetDefinitionDTO;
import com.backend.water_management_system.dashboard.dto.WidgetDefinitionRequest;
import com.backend.water_management_system.dashboard.service.WidgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Widget catalog and management endpoints. All write operations are Super Admin only.
 *
 * <p>GET  /api/widgets/catalog                       — list all widgets (SUPER_ADMIN)
 * <p>POST /api/widgets                               — create a widget definition (SUPER_ADMIN)
 * <p>PUT  /api/widgets/{id}                          — update a widget definition (SUPER_ADMIN)
 * <p>DELETE /api/widgets/{id}                        — deactivate a widget (SUPER_ADMIN)
 * <p>PUT  /api/dashboards/{dashboardId}/widgets      — update dashboard layout (SUPER_ADMIN)
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class WidgetController {

    private final WidgetService widgetService;

    @GetMapping("/api/widgets/catalog")
    public ResponseEntity<List<WidgetDefinitionDTO>> getCatalog() {
        return ResponseEntity.ok(widgetService.getAllWidgets());
    }

    @PostMapping("/api/widgets")
    public ResponseEntity<WidgetDefinitionDTO> createWidget(
            @Valid @RequestBody WidgetDefinitionRequest request) {
        try {
            return ResponseEntity.status(201).body(widgetService.createWidget(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/api/widgets/{id}")
    public ResponseEntity<WidgetDefinitionDTO> updateWidget(
            @PathVariable Long id,
            @Valid @RequestBody WidgetDefinitionRequest request) {
        try {
            return ResponseEntity.ok(widgetService.updateWidget(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/api/widgets/{id}")
    public ResponseEntity<Void> deactivateWidget(@PathVariable Long id) {
        try {
            widgetService.deactivateWidget(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/api/dashboards/{dashboardId}/widgets")
    public ResponseEntity<List<DashboardWidgetDTO>> updateDashboardLayout(
            @PathVariable Long dashboardId,
            @RequestBody List<Map<String, Object>> placements) {
        try {
            return ResponseEntity.ok(widgetService.updateDashboardLayout(dashboardId, placements));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
