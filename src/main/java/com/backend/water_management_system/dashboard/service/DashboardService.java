package com.backend.water_management_system.dashboard.service;

import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.customer.service.CustomerAccessService;
import com.backend.water_management_system.dashboard.dto.CustomerDashboardSummaryDTO;
import com.backend.water_management_system.dashboard.dto.DashboardConfigDTO;
import com.backend.water_management_system.dashboard.dto.DashboardWidgetDTO;
import com.backend.water_management_system.dashboard.dto.SystemDashboardSummaryDTO;
import com.backend.water_management_system.dashboard.entity.DashboardDefinition;
import com.backend.water_management_system.dashboard.entity.DashboardWidget;
import com.backend.water_management_system.dashboard.repository.DashboardDefinitionRepository;
import com.backend.water_management_system.dashboard.repository.DashboardWidgetRepository;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.inquiry.repository.InquiryRepository;
import com.backend.water_management_system.payments.enums.SlipStatus;
import com.backend.water_management_system.payments.repository.BankSlipRepository;
import com.backend.water_management_system.payments.repository.PaymentRepository;
import com.backend.water_management_system.security.UserPrincipal;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides the dashboard configuration and aggregated summary data.
 *
 * <p>This service is an aggregation/presentation layer over the existing domain services.
 * It does NOT duplicate business logic — it calls existing repositories directly for
 * read-only aggregation queries, which is appropriate for dashboard summary statistics.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardDefinitionRepository dashboardRepo;
    private final DashboardWidgetRepository dashboardWidgetRepo;
    private final CustomerRepository customerRepository;
    private final CustomerAccessService customerAccessService;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final BankSlipRepository bankSlipRepository;
    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    // ── Dashboard Configuration ────────────────────────────────────────────

    /**
     * Returns the active dashboard configuration for the authenticated user's role.
     * Only visible widget placements are included.
     */
    public DashboardConfigDTO getDashboardForPrincipal(UserPrincipal principal) {
        Role role = principal.getUser().getRole();
        DashboardDefinition dashboard = dashboardRepo.findByAssignedRoleAndActiveTrue(role)
                .orElseThrow(() -> new RuntimeException(
                        "No active dashboard configured for role: " + role));

        List<DashboardWidget> placements =
                dashboardWidgetRepo.findByDashboard_IdAndVisibleTrueOrderByPositionAsc(dashboard.getId());

        List<DashboardWidgetDTO> widgetDTOs = placements.stream()
                .filter(dw -> dw.getWidget().isActive())
                .map(this::toWidgetDTO)
                .collect(Collectors.toList());

        return DashboardConfigDTO.builder()
                .dashboardId(dashboard.getId())
                .dashboardKey(dashboard.getDashboardKey())
                .name(dashboard.getName())
                .assignedRole(role)
                .version(dashboard.getVersion())
                .widgets(widgetDTOs)
                .build();
    }

    private DashboardWidgetDTO toWidgetDTO(DashboardWidget dw) {
        return DashboardWidgetDTO.builder()
                .id(dw.getId())
                .widgetKey(dw.getWidget().getWidgetKey())
                .name(dw.getWidget().getName())
                .description(dw.getWidget().getDescription())
                .widgetType(dw.getWidget().getWidgetType().name())
                .componentKey(dw.getWidget().getComponentKey())
                .position(dw.getPosition())
                .colSpan(dw.getColSpan())
                .rowSpan(dw.getRowSpan())
                .visible(dw.isVisible())
                .configJson(dw.getConfigJson())
                .build();
    }

    // ── System Summary ─────────────────────────────────────────────────────

    /**
     * Builds a system-wide summary using DB-level COUNT/SUM queries.
     * No collections are loaded into Java memory for calculation.
     */
    public SystemDashboardSummaryDTO getSystemSummary() {
        LocalDate now = LocalDate.now();
        String period = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        long customerCount = customerRepository.count();

        long activeAdminCount =
                userRepository.countByRole(Role.SUPER_ADMIN) +
                userRepository.countByRole(Role.SYSTEM_ADMIN) +
                userRepository.countByRole(Role.CUSTOMER_HANDLER) +
                userRepository.countByRole(Role.METER_READER);

        long pendingSlipCount = bankSlipRepository.countByStatus(SlipStatus.PENDING);
        BigDecimal pendingSlipAmount = bankSlipRepository.sumPendingSlipAmount();

        long outstandingBillCount = billRepository.countOutstandingBills();
        BigDecimal outstandingAmount = billRepository.sumOutstandingAmount();

        BigDecimal paidThisMonth = paymentRepository
                .sumCompletedPaymentsByMonth(now.getYear(), now.getMonthValue());

        long openInquiryCount = inquiryRepository.countByStatus("open");

        return SystemDashboardSummaryDTO.builder()
                .period(period)
                .customerCount(customerCount)
                .activeAdminCount(activeAdminCount)
                .pendingSlipCount(pendingSlipCount)
                .pendingSlipAmount(pendingSlipAmount != null ? pendingSlipAmount : BigDecimal.ZERO)
                .outstandingBillCount(outstandingBillCount)
                .outstandingAmount(outstandingAmount != null ? outstandingAmount : BigDecimal.ZERO)
                .paidThisMonth(paidThisMonth != null ? paidThisMonth : BigDecimal.ZERO)
                .openInquiryCount(openInquiryCount)
                .build();
    }

    // ── Customer Summary ───────────────────────────────────────────────────

    /**
     * Returns a customer-scoped summary for the authenticated customer.
     * Enforces ownership — the customer can never request another customer's summary.
     */
    public CustomerDashboardSummaryDTO getCustomerSummary(UserPrincipal principal) {
        if (principal.getUser().getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Only customers can access this summary.");
        }
        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);
        Customer customer = customerRepository.findBySubscriptionNumber(subscriptionNumber)
                .orElseThrow(() -> new AccessDeniedException("Customer not found"));

        BigDecimal totalPendingBalance = billRepository.getTotalPendingBalance(subscriptionNumber);
        long unpaidBillCount = billRepository.countPendingBillsBySubscription(subscriptionNumber);
        long pendingSlipCount = bankSlipRepository
                .countBySubscriptionNumberAndStatus(subscriptionNumber, SlipStatus.PENDING);
        long openInquiryCount = customer.getUser().getEmail() != null
                ? inquiryRepository.countByEmailAndStatus(customer.getUser().getEmail(), "open")
                : 0L;

        return CustomerDashboardSummaryDTO.builder()
                .subscriptionNumber(subscriptionNumber)
                .accountHolderName(customer.getAccountHolderName())
                .connectionType(customer.getConnectionType())
                .totalPendingBalance(totalPendingBalance != null ? totalPendingBalance : BigDecimal.ZERO)
                .unpaidBillCount(unpaidBillCount)
                .pendingSlipCount(pendingSlipCount)
                .openInquiryCount(openInquiryCount)
                .build();
    }
}
