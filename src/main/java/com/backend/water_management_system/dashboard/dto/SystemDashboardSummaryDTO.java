package com.backend.water_management_system.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Aggregated system-wide summary statistics for the admin dashboard header widgets.
 * All values come from database COUNT/SUM aggregation queries — never from in-memory collection operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemDashboardSummaryDTO {
    /** Current billing period (e.g. "2026-08"). */
    private String period;

    /** Total number of registered customers. */
    private long customerCount;

    /** Total number of active admin users (all admin roles). */
    private long activeAdminCount;

    /** Number of bank slips awaiting review. */
    private long pendingSlipCount;

    /** Total monetary amount of pending bank slips. */
    private BigDecimal pendingSlipAmount;

    /** Number of bills with outstanding balance. */
    private long outstandingBillCount;

    /** Total outstanding amount across all pending bills. */
    private BigDecimal outstandingAmount;

    /** Total amount paid in the current calendar month. */
    private BigDecimal paidThisMonth;

    /** Number of open inquiries (status = "open"). */
    private long openInquiryCount;

    /** Number of active (non-dismissed) alerts. */
    private long activeAlertCount;
}
