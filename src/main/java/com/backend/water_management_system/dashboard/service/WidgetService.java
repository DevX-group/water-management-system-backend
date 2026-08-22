package com.backend.water_management_system.dashboard.service;

import com.backend.water_management_system.dashboard.dto.DashboardWidgetDTO;
import com.backend.water_management_system.dashboard.dto.WidgetDefinitionDTO;
import com.backend.water_management_system.dashboard.dto.WidgetDefinitionRequest;
import com.backend.water_management_system.dashboard.entity.DashboardDefinition;
import com.backend.water_management_system.dashboard.entity.DashboardWidget;
import com.backend.water_management_system.dashboard.entity.WidgetDefinition;
import com.backend.water_management_system.dashboard.enums.WidgetType;
import com.backend.water_management_system.dashboard.repository.DashboardDefinitionRepository;
import com.backend.water_management_system.dashboard.repository.DashboardWidgetRepository;
import com.backend.water_management_system.dashboard.repository.WidgetDefinitionRepository;
import com.backend.water_management_system.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages widget and dashboard definitions. All write operations are restricted to SUPER_ADMIN
 * (enforced at the controller layer via @PreAuthorize).
 */
@Service
@RequiredArgsConstructor
public class WidgetService {

    private final WidgetDefinitionRepository widgetRepo;
    private final DashboardDefinitionRepository dashboardRepo;
    private final DashboardWidgetRepository dashboardWidgetRepo;

    /** Allowed component keys (frontend allow-list). The frontend registry must have matching entries. */
    private static final Set<String> ALLOWED_COMPONENT_KEYS = Set.of(
            "customer-current-bill",
            "customer-outstanding",
            "customer-pay-now",
            "customer-usage-trend",
            "customer-recent-payments",
            "customer-notifications",
            "customer-bank-slip-status",
            "customer-inquiries",
            "meter-quick-entry",
            "meter-latest-reading",
            "meter-reading-history",
            "internal-chat-link",
            "handler-pending-slips",
            "handler-recent-payments",
            "handler-open-inquiries",
            "handler-customer-search",
            "admin-system-summary",
            "admin-usage-chart",
            "admin-revenue-chart",
            "admin-alerts",
            "admin-messaging-link",
            "admin-blogs-link",
            "admin-predictions-link",
            "superadmin-admin-count",
            "superadmin-region-summary",
            "superadmin-widget-management-link",
            "superadmin-user-management-link",
            "quick-link"
    );

    // ── Widget Catalog ─────────────────────────────────────────────────────

    public List<WidgetDefinitionDTO> getAllWidgets() {
        return widgetRepo.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<WidgetDefinitionDTO> getActiveWidgets() {
        return widgetRepo.findByActiveTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Widget CRUD (Super Admin only) ─────────────────────────────────────

    @Transactional
    public WidgetDefinitionDTO createWidget(WidgetDefinitionRequest request) {
        validateRequest(request);
        if (widgetRepo.existsByWidgetKey(request.getWidgetKey())) {
            throw new IllegalArgumentException("Widget key already exists: " + request.getWidgetKey());
        }
        WidgetDefinition widget = buildFromRequest(request, new WidgetDefinition());
        return toDTO(widgetRepo.save(widget));
    }

    @Transactional
    public WidgetDefinitionDTO updateWidget(Long id, WidgetDefinitionRequest request) {
        validateRequest(request);
        WidgetDefinition widget = widgetRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Widget not found: " + id));
                
        buildFromRequest(request, widget);
        return toDTO(widgetRepo.save(widget));
    }

    @Transactional
    public void deactivateWidget(Long id) {
        WidgetDefinition widget = widgetRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Widget not found: " + id));
        widget.setActive(false);
        widgetRepo.save(widget);
        
        // Remove widget from all dashboards when deactivated
        dashboardWidgetRepo.deleteByWidget_Id(id);
    }

    // ── Dashboard Layout (Super Admin only) ────────────────────────────────

    @Transactional
    public void addWidgetToDashboardRole(Role role, Long widgetId) {
        DashboardDefinition dashboard = dashboardRepo.findByAssignedRoleAndActiveTrue(role)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found for role: " + role));
        WidgetDefinition widget = widgetRepo.findById(widgetId)
                .orElseThrow(() -> new IllegalArgumentException("Widget not found: " + widgetId));

        // Let's verify allowed roles
        if (!widget.getAllowedRoles().contains(role)) {
            throw new IllegalArgumentException("Widget " + widget.getWidgetKey() + " is not allowed for role " + role);
        }

        if (dashboardWidgetRepo.existsByDashboard_IdAndWidget_Id(dashboard.getId(), widgetId)) {
            return; // Already exists
        }

        int maxPos = dashboardWidgetRepo.findByDashboard_IdOrderByPositionAsc(dashboard.getId())
                .stream().mapToInt(DashboardWidget::getPosition).max().orElse(-1);

        DashboardWidget dw = DashboardWidget.builder()
                .dashboard(dashboard)
                .widget(widget)
                .position(maxPos + 1)
                .colSpan(widget.getDefaultColSpan())
                .rowSpan(widget.getDefaultRowSpan())
                .visible(true)
                .build();
        dashboardWidgetRepo.save(dw);
    }

    @Transactional
    public void removeWidgetFromDashboardRole(Role role, Long widgetId) {
        DashboardDefinition dashboard = dashboardRepo.findByAssignedRoleAndActiveTrue(role)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found for role: " + role));
        
        dashboardWidgetRepo.findByDashboard_IdOrderByPositionAsc(dashboard.getId())
                .stream()
                .filter(dw -> dw.getWidget().getId().equals(widgetId))
                .findFirst()
                .ifPresent(dashboardWidgetRepo::delete);
    }

    /**
     * Replaces the widget layout of a dashboard.
     * Validates each widget placement before persisting.
     */
    @Transactional
    public List<DashboardWidgetDTO> updateDashboardLayout(Long dashboardId, List<Map<String, Object>> placements) {
        DashboardDefinition dashboard = dashboardRepo.findById(dashboardId)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found: " + dashboardId));

        dashboardWidgetRepo.deleteByDashboard_Id(dashboardId);
        dashboardWidgetRepo.flush();

        int position = 0;
        for (Map<String, Object> p : placements) {
            if (!p.containsKey("widgetId") || p.get("widgetId") == null) {
                throw new IllegalArgumentException("widgetId is required in layout placement");
            }
            Long widgetId = ((Number) p.get("widgetId")).longValue();
            int colSpan = p.containsKey("colSpan") ? ((Number) p.get("colSpan")).intValue() : 1;
            int rowSpan = p.containsKey("rowSpan") ? ((Number) p.get("rowSpan")).intValue() : 1;
            boolean visible = !p.containsKey("visible") || (Boolean) p.get("visible");
            String configJson = p.containsKey("configJson") ? (String) p.get("configJson") : null;

            if (colSpan < 1 || colSpan > 4 || rowSpan < 1 || rowSpan > 4) {
                throw new IllegalArgumentException("colSpan/rowSpan must be 1–4");
            }

            WidgetDefinition widget = widgetRepo.findById(widgetId)
                    .orElseThrow(() -> new IllegalArgumentException("Widget not found: " + widgetId));

            // Ensure the widget's allowed roles include the dashboard's role
            if (!widget.getAllowedRoles().contains(dashboard.getAssignedRole())) {
                throw new IllegalArgumentException("Widget " + widget.getWidgetKey()
                        + " is not authorised for role " + dashboard.getAssignedRole());
            }

            DashboardWidget dw = DashboardWidget.builder()
                    .dashboard(dashboard)
                    .widget(widget)
                    .position(position++)
                    .colSpan(colSpan)
                    .rowSpan(rowSpan)
                    .visible(visible)
                    .configJson(configJson)
                    .build();
            dashboardWidgetRepo.save(dw);
        }

        return dashboardWidgetRepo.findByDashboard_IdOrderByPositionAsc(dashboardId)
                .stream().map(this::toPlacementDTO).collect(Collectors.toList());
    }

    // ── Validation ─────────────────────────────────────────────────────────

    private void validateRequest(WidgetDefinitionRequest request) {
        // Validate widgetType against enum
        try {
            WidgetType.valueOf(request.getWidgetType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid widgetType: " + request.getWidgetType());
        }
        // Validate componentKey against allow-list
        if (!ALLOWED_COMPONENT_KEYS.contains(request.getComponentKey())) {
            throw new IllegalArgumentException("Unknown componentKey: " + request.getComponentKey());
        }
        // Validate allowedRoles
        if (request.getAllowedRoles() != null) {
            for (String roleStr : request.getAllowedRoles()) {
                try {
                    Role.valueOf(roleStr);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid role: " + roleStr);
                }
            }
        }
    }

    // ── Mapping ────────────────────────────────────────────────────────────

    private WidgetDefinition buildFromRequest(WidgetDefinitionRequest request, WidgetDefinition widget) {
        widget.setWidgetKey(request.getWidgetKey());
        widget.setName(request.getName());
        widget.setDescription(request.getDescription());
        widget.setWidgetType(WidgetType.valueOf(request.getWidgetType()));
        widget.setComponentKey(request.getComponentKey());
        widget.setActive(request.isActive());
        widget.setDefaultColSpan(request.getDefaultColSpan());
        widget.setDefaultRowSpan(request.getDefaultRowSpan());
        Set<Role> roles = new HashSet<>();
        if (request.getAllowedRoles() != null) {
            for (String r : request.getAllowedRoles()) {
                roles.add(Role.valueOf(r));
            }
        }
        widget.setAllowedRoles(roles);
        return widget;
    }

    private WidgetDefinitionDTO toDTO(WidgetDefinition w) {
        return WidgetDefinitionDTO.builder()
                .id(w.getId())
                .widgetKey(w.getWidgetKey())
                .name(w.getName())
                .description(w.getDescription())
                .widgetType(w.getWidgetType())
                .componentKey(w.getComponentKey())
                .active(w.isActive())
                .allowedRoles(w.getAllowedRoles())
                .defaultColSpan(w.getDefaultColSpan())
                .defaultRowSpan(w.getDefaultRowSpan())
                .version(w.getVersion())
                .build();
    }

    private DashboardWidgetDTO toPlacementDTO(DashboardWidget dw) {
        return DashboardWidgetDTO.builder()
                .id(dw.getId())
                .widgetId(dw.getWidget().getId())
                .widgetKey(dw.getWidget().getWidgetKey())
                .name(dw.getWidget().getName())
                .widgetType(dw.getWidget().getWidgetType().name())
                .componentKey(dw.getWidget().getComponentKey())
                .position(dw.getPosition())
                .colSpan(dw.getColSpan())
                .rowSpan(dw.getRowSpan())
                .visible(dw.isVisible())
                .configJson(dw.getConfigJson())
                .build();
    }
}
