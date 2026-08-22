package com.backend.water_management_system.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Aggregated customer-scoped summary for the customer dashboard header widgets.
 * Scoped to a single subscription — never returns other customers' data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDashboardSummaryDTO {
    private String subscriptionNumber;
    private String accountHolderName;
    private String connectionType;

    /** Total pending balance across all unpaid bills (current + outstanding). */
    private BigDecimal totalPendingBalance;

    /** Number of unpaid bills. */
    private long unpaidBillCount;

    /** Number of bank slips in PENDING status. */
    private long pendingSlipCount;

    /** Number of open inquiries submitted by this customer. */
    private long openInquiryCount;
}
