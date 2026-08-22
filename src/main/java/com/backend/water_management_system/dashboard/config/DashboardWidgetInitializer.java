package com.backend.water_management_system.dashboard.config;

import com.backend.water_management_system.dashboard.entity.DashboardDefinition;
import com.backend.water_management_system.dashboard.entity.DashboardWidget;
import com.backend.water_management_system.dashboard.entity.WidgetDefinition;
import com.backend.water_management_system.dashboard.enums.WidgetType;
import com.backend.water_management_system.dashboard.repository.DashboardDefinitionRepository;
import com.backend.water_management_system.dashboard.repository.DashboardWidgetRepository;
import com.backend.water_management_system.dashboard.repository.WidgetDefinitionRepository;
import com.backend.water_management_system.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Seeds the default dashboard and widget definitions on first startup.
 * This runner is idempotent — it only seeds if no widget definitions exist.
 *
 * <p>Ordered after DataSeeder (@Order(2)) so the user/customer seed data runs first.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class DashboardWidgetInitializer implements CommandLineRunner {

    private final WidgetDefinitionRepository widgetRepo;
    private final DashboardDefinitionRepository dashboardRepo;
    private final DashboardWidgetRepository dashboardWidgetRepo;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (widgetRepo.count() > 0) {
            return; // Already seeded
        }

        // ── 1. Create widget definitions ──────────────────────────────────

        // --- Customer Widgets ---
        WidgetDefinition currentBill = save(WidgetDefinition.builder()
                .widgetKey("customer-current-bill")
                .name("Current Bill")
                .description("Shows the latest bill for the authenticated customer.")
                .widgetType(WidgetType.STAT)
                .componentKey("customer-current-bill")
                .allowedRoles(Set.of(Role.CUSTOMER))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition outstandingBalance = save(WidgetDefinition.builder()
                .widgetKey("customer-outstanding")
                .name("Outstanding Balance")
                .description("Total unpaid balance across all bills.")
                .widgetType(WidgetType.STAT)
                .componentKey("customer-outstanding")
                .allowedRoles(Set.of(Role.CUSTOMER))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition payNow = save(WidgetDefinition.builder()
                .widgetKey("customer-pay-now")
                .name("Pay Now")
                .description("Quick action to initiate a payment.")
                .widgetType(WidgetType.ACTION)
                .componentKey("customer-pay-now")
                .allowedRoles(Set.of(Role.CUSTOMER))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition usageTrend = save(WidgetDefinition.builder()
                .widgetKey("customer-usage-trend")
                .name("Usage Trend")
                .description("Monthly water usage chart for the current year.")
                .widgetType(WidgetType.CHART)
                .componentKey("customer-usage-trend")
                .allowedRoles(Set.of(Role.CUSTOMER))
                .defaultColSpan(2).defaultRowSpan(2).build());

        WidgetDefinition recentPayments = save(WidgetDefinition.builder()
                .widgetKey("customer-recent-payments")
                .name("Recent Payments")
                .description("Last 5 payments made by the customer.")
                .widgetType(WidgetType.LIST)
                .componentKey("customer-recent-payments")
                .allowedRoles(Set.of(Role.CUSTOMER))
                .defaultColSpan(2).defaultRowSpan(2).build());

        WidgetDefinition notifications = save(WidgetDefinition.builder()
                .widgetKey("customer-notifications")
                .name("Notifications")
                .description("Recent notifications for the customer.")
                .widgetType(WidgetType.LIST)
                .componentKey("customer-notifications")
                .allowedRoles(Set.of(Role.CUSTOMER))
                .defaultColSpan(1).defaultRowSpan(2).build());

        WidgetDefinition bankSlipStatus = save(WidgetDefinition.builder()
                .widgetKey("customer-bank-slip-status")
                .name("Bank Slip Status")
                .description("Status of submitted bank slips.")
                .widgetType(WidgetType.LIST)
                .componentKey("customer-bank-slip-status")
                .allowedRoles(Set.of(Role.CUSTOMER))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition customerInquiries = save(WidgetDefinition.builder()
                .widgetKey("customer-inquiries")
                .name("My Inquiries")
                .description("View and submit support inquiries.")
                .widgetType(WidgetType.ACTION)
                .componentKey("customer-inquiries")
                .allowedRoles(Set.of(Role.CUSTOMER))
                .defaultColSpan(1).defaultRowSpan(1).build());

        // --- Meter Reader Widgets ---
        WidgetDefinition meterQuickEntry = save(WidgetDefinition.builder()
                .widgetKey("meter-quick-entry")
                .name("Quick Meter Entry")
                .description("Navigate to the meter reading entry form.")
                .widgetType(WidgetType.ACTION)
                .componentKey("meter-quick-entry")
                .allowedRoles(Set.of(Role.METER_READER, Role.SUPER_ADMIN, Role.SYSTEM_ADMIN))
                .defaultColSpan(2).defaultRowSpan(1).build());

        WidgetDefinition latestReading = save(WidgetDefinition.builder()
                .widgetKey("meter-latest-reading")
                .name("Today's Readings")
                .description("Meter readings submitted today.")
                .widgetType(WidgetType.LIST)
                .componentKey("meter-latest-reading")
                .allowedRoles(Set.of(Role.METER_READER, Role.SUPER_ADMIN, Role.SYSTEM_ADMIN))
                .defaultColSpan(2).defaultRowSpan(2).build());

        WidgetDefinition meterHistory = save(WidgetDefinition.builder()
                .widgetKey("meter-reading-history")
                .name("Reading History")
                .description("Recent meter reading history.")
                .widgetType(WidgetType.TABLE)
                .componentKey("meter-reading-history")
                .allowedRoles(Set.of(Role.METER_READER, Role.SUPER_ADMIN, Role.SYSTEM_ADMIN))
                .defaultColSpan(2).defaultRowSpan(2).build());

        // --- Shared Admin Widgets ---
        WidgetDefinition internalChatLink = save(WidgetDefinition.builder()
                .widgetKey("internal-chat-link")
                .name("Internal Chat")
                .description("Open the internal staff chat.")
                .widgetType(WidgetType.ACTION)
                .componentKey("internal-chat-link")
                .allowedRoles(Set.of(Role.METER_READER, Role.CUSTOMER_HANDLER,
                        Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(1).defaultRowSpan(1).build());

        // --- Customer Handler Widgets ---
        WidgetDefinition pendingSlips = save(WidgetDefinition.builder()
                .widgetKey("handler-pending-slips")
                .name("Pending Bank Slips")
                .description("Count and list of bank slips awaiting review.")
                .widgetType(WidgetType.STAT)
                .componentKey("handler-pending-slips")
                .allowedRoles(Set.of(Role.CUSTOMER_HANDLER, Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition handlerRecentPayments = save(WidgetDefinition.builder()
                .widgetKey("handler-recent-payments")
                .name("Recent Payments")
                .description("Latest payments across all customers.")
                .widgetType(WidgetType.LIST)
                .componentKey("handler-recent-payments")
                .allowedRoles(Set.of(Role.CUSTOMER_HANDLER, Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(2).defaultRowSpan(2).build());

        WidgetDefinition openInquiries = save(WidgetDefinition.builder()
                .widgetKey("handler-open-inquiries")
                .name("Open Inquiries")
                .description("Number of unresolved customer inquiries.")
                .widgetType(WidgetType.STAT)
                .componentKey("handler-open-inquiries")
                .allowedRoles(Set.of(Role.CUSTOMER_HANDLER, Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition customerSearch = save(WidgetDefinition.builder()
                .widgetKey("handler-customer-search")
                .name("Customer Management")
                .description("Navigate to customer search and management.")
                .widgetType(WidgetType.ACTION)
                .componentKey("handler-customer-search")
                .allowedRoles(Set.of(Role.CUSTOMER_HANDLER, Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(1).defaultRowSpan(1).build());

        // --- System Admin Widgets ---
        WidgetDefinition systemSummary = save(WidgetDefinition.builder()
                .widgetKey("admin-system-summary")
                .name("System Summary")
                .description("High-level system statistics including customers, revenue, outstanding amounts.")
                .widgetType(WidgetType.STAT)
                .componentKey("admin-system-summary")
                .allowedRoles(Set.of(Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(4).defaultRowSpan(1).build());

        WidgetDefinition usageChart = save(WidgetDefinition.builder()
                .widgetKey("admin-usage-chart")
                .name("System Usage Trend")
                .description("Monthly water usage chart for the entire system.")
                .widgetType(WidgetType.CHART)
                .componentKey("admin-usage-chart")
                .allowedRoles(Set.of(Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(2).defaultRowSpan(2).build());

        WidgetDefinition revenueChart = save(WidgetDefinition.builder()
                .widgetKey("admin-revenue-chart")
                .name("Monthly Revenue")
                .description("Monthly revenue and billing chart.")
                .widgetType(WidgetType.CHART)
                .componentKey("admin-revenue-chart")
                .allowedRoles(Set.of(Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(2).defaultRowSpan(2).build());

        WidgetDefinition alertsWidget = save(WidgetDefinition.builder()
                .widgetKey("admin-alerts")
                .name("Active Alerts")
                .description("System alerts that have not been dismissed.")
                .widgetType(WidgetType.ALERT)
                .componentKey("admin-alerts")
                .allowedRoles(Set.of(Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(2).defaultRowSpan(1).build());

        WidgetDefinition messagingLink = save(WidgetDefinition.builder()
                .widgetKey("admin-messaging-link")
                .name("Messaging")
                .description("Send bulk SMS/notifications to customers.")
                .widgetType(WidgetType.ACTION)
                .componentKey("admin-messaging-link")
                .allowedRoles(Set.of(Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition blogsLink = save(WidgetDefinition.builder()
                .widgetKey("admin-blogs-link")
                .name("Blog Management")
                .description("Manage blog posts and announcements.")
                .widgetType(WidgetType.ACTION)
                .componentKey("admin-blogs-link")
                .allowedRoles(Set.of(Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition predictionsLink = save(WidgetDefinition.builder()
                .widgetKey("admin-predictions-link")
                .name("Predictions")
                .description("AI-powered usage and billing predictions.")
                .widgetType(WidgetType.ACTION)
                .componentKey("admin-predictions-link")
                .allowedRoles(Set.of(Role.SYSTEM_ADMIN, Role.SUPER_ADMIN))
                .defaultColSpan(1).defaultRowSpan(1).build());

        // --- Super Admin Widgets ---
        WidgetDefinition widgetMgmtLink = save(WidgetDefinition.builder()
                .widgetKey("superadmin-widget-management-link")
                .name("Widget Management")
                .description("Configure and manage dashboard widgets.")
                .widgetType(WidgetType.ACTION)
                .componentKey("superadmin-widget-management-link")
                .allowedRoles(Set.of(Role.SUPER_ADMIN))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition userMgmtLink = save(WidgetDefinition.builder()
                .widgetKey("superadmin-user-management-link")
                .name("Admin Management")
                .description("Manage admin users and roles.")
                .widgetType(WidgetType.ACTION)
                .componentKey("superadmin-user-management-link")
                .allowedRoles(Set.of(Role.SUPER_ADMIN, Role.SYSTEM_ADMIN))
                .defaultColSpan(1).defaultRowSpan(1).build());

        WidgetDefinition regionSummary = save(WidgetDefinition.builder()
                .widgetKey("superadmin-region-summary")
                .name("Region Summary")
                .description("Customer distribution by region.")
                .widgetType(WidgetType.LIST)
                .componentKey("superadmin-region-summary")
                .allowedRoles(Set.of(Role.SUPER_ADMIN, Role.SYSTEM_ADMIN))
                .defaultColSpan(2).defaultRowSpan(2).build());

        // ── 2. Create dashboard definitions ──────────────────────────────

        DashboardDefinition customerDash = saveDashboard(DashboardDefinition.builder()
                .dashboardKey("dashboard-customer")
                .name("Customer Dashboard")
                .assignedRole(Role.CUSTOMER)
                .build());

        DashboardDefinition meterDash = saveDashboard(DashboardDefinition.builder()
                .dashboardKey("dashboard-meter-reader")
                .name("Meter Reader Dashboard")
                .assignedRole(Role.METER_READER)
                .build());

        DashboardDefinition handlerDash = saveDashboard(DashboardDefinition.builder()
                .dashboardKey("dashboard-customer-handler")
                .name("Customer Handler Dashboard")
                .assignedRole(Role.CUSTOMER_HANDLER)
                .build());

        DashboardDefinition sysAdminDash = saveDashboard(DashboardDefinition.builder()
                .dashboardKey("dashboard-system-admin")
                .name("System Admin Dashboard")
                .assignedRole(Role.SYSTEM_ADMIN)
                .build());

        DashboardDefinition superAdminDash = saveDashboard(DashboardDefinition.builder()
                .dashboardKey("dashboard-super-admin")
                .name("Super Admin Dashboard")
                .assignedRole(Role.SUPER_ADMIN)
                .build());

        // ── 3. Assign widgets to dashboards ───────────────────────────────

        // Customer Dashboard
        place(customerDash, currentBill, 0, 1, 1);
        place(customerDash, outstandingBalance, 1, 1, 1);
        place(customerDash, payNow, 2, 1, 1);
        place(customerDash, bankSlipStatus, 3, 1, 1);
        place(customerDash, usageTrend, 4, 2, 2);
        place(customerDash, recentPayments, 5, 2, 2);
        place(customerDash, notifications, 6, 1, 2);
        place(customerDash, customerInquiries, 7, 1, 1);

        // Meter Reader Dashboard
        place(meterDash, meterQuickEntry, 0, 2, 1);
        place(meterDash, latestReading, 1, 2, 2);
        place(meterDash, meterHistory, 2, 2, 2);
        place(meterDash, internalChatLink, 3, 1, 1);

        // Customer Handler Dashboard
        place(handlerDash, pendingSlips, 0, 1, 1);
        place(handlerDash, openInquiries, 1, 1, 1);
        place(handlerDash, customerSearch, 2, 1, 1);
        place(handlerDash, internalChatLink, 3, 1, 1);
        place(handlerDash, handlerRecentPayments, 4, 2, 2);

        // System Admin Dashboard
        place(sysAdminDash, systemSummary, 0, 4, 1);
        place(sysAdminDash, pendingSlips, 1, 1, 1);
        place(sysAdminDash, openInquiries, 2, 1, 1);
        place(sysAdminDash, alertsWidget, 3, 2, 1);
        place(sysAdminDash, usageChart, 4, 2, 2);
        place(sysAdminDash, revenueChart, 5, 2, 2);
        place(sysAdminDash, handlerRecentPayments, 6, 2, 2);
        place(sysAdminDash, messagingLink, 7, 1, 1);
        place(sysAdminDash, blogsLink, 8, 1, 1);
        place(sysAdminDash, predictionsLink, 9, 1, 1);
        place(sysAdminDash, internalChatLink, 10, 1, 1);
        place(sysAdminDash, userMgmtLink, 11, 1, 1);
        place(sysAdminDash, regionSummary, 12, 2, 2);

        // Super Admin Dashboard (extends system admin)
        place(superAdminDash, systemSummary, 0, 4, 1);
        place(superAdminDash, pendingSlips, 1, 1, 1);
        place(superAdminDash, openInquiries, 2, 1, 1);
        place(superAdminDash, alertsWidget, 3, 2, 1);
        place(superAdminDash, usageChart, 4, 2, 2);
        place(superAdminDash, revenueChart, 5, 2, 2);
        place(superAdminDash, handlerRecentPayments, 6, 2, 2);
        place(superAdminDash, regionSummary, 7, 2, 2);
        place(superAdminDash, widgetMgmtLink, 8, 1, 1);
        place(superAdminDash, userMgmtLink, 9, 1, 1);
        place(superAdminDash, messagingLink, 10, 1, 1);
        place(superAdminDash, predictionsLink, 11, 1, 1);
        place(superAdminDash, internalChatLink, 12, 1, 1);

        System.out.println("[DashboardWidgetInitializer] Seeded default dashboard and widget configurations.");
    }

    private WidgetDefinition save(WidgetDefinition w) {
        return widgetRepo.save(w);
    }

    private DashboardDefinition saveDashboard(DashboardDefinition d) {
        return dashboardRepo.save(d);
    }

    private void place(DashboardDefinition dashboard, WidgetDefinition widget,
                       int position, int colSpan, int rowSpan) {
        DashboardWidget dw = DashboardWidget.builder()
                .dashboard(dashboard)
                .widget(widget)
                .position(position)
                .colSpan(colSpan)
                .rowSpan(rowSpan)
                .visible(true)
                .build();
        dashboardWidgetRepo.save(dw);
    }
}
