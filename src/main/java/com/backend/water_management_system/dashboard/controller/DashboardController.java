package com.backend.water_management_system.dashboard.controller;

import com.backend.water_management_system.dashboard.dto.CustomerDashboardSummaryDTO;
import com.backend.water_management_system.dashboard.dto.DashboardConfigDTO;
import com.backend.water_management_system.dashboard.dto.SystemDashboardSummaryDTO;
import com.backend.water_management_system.dashboard.service.DashboardService;
import com.backend.water_management_system.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard configuration and summary data endpoints.
 *
 * <p>GET /api/dashboards/me          — returns the role-appropriate dashboard config (all roles)
 * <p>GET /api/dashboards/summary/system  — aggregated system stats (SUPER_ADMIN, SYSTEM_ADMIN)
 * <p>GET /api/dashboards/summary/customer — customer-scoped stats (CUSTOMER only)
 */
@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Returns the active dashboard configuration for the authenticated user's role.
     * Available to all authenticated users — each gets their role-appropriate dashboard.
     */
    @GetMapping("/me")
    public ResponseEntity<DashboardConfigDTO> getMyDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getDashboardForPrincipal(principal));
    }

    /**
     * Returns system-wide aggregated statistics.
     * Restricted to SUPER_ADMIN and SYSTEM_ADMIN — meter readers and handlers cannot access this.
     */
    @GetMapping("/summary/system")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<SystemDashboardSummaryDTO> getSystemSummary() {
        return ResponseEntity.ok(dashboardService.getSystemSummary());
    }

    /**
     * Returns aggregated statistics scoped to the authenticated customer.
     * Customers can only see their own data — ownership is enforced in the service layer.
     */
    @GetMapping("/summary/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerDashboardSummaryDTO> getCustomerSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getCustomerSummary(principal));
    }
}
