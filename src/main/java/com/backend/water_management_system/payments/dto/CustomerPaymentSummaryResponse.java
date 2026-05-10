package com.backend.water_management_system.payments.dto;

import java.math.BigDecimal;

public class CustomerPaymentSummaryResponse {
    private String subscriptionNumber;
    private BigDecimal monthlyDue;
    private BigDecimal outstandingBalance;
    private BigDecimal totalDue;
    private String billStatus;

    public CustomerPaymentSummaryResponse() {}

    public CustomerPaymentSummaryResponse(String subscriptionNumber, BigDecimal monthlyDue, BigDecimal outstandingBalance, BigDecimal totalDue, String billStatus) {
        this.subscriptionNumber = subscriptionNumber;
        this.monthlyDue = monthlyDue;
        this.outstandingBalance = outstandingBalance;
        this.totalDue = totalDue;
        this.billStatus = billStatus;
    }

    public String getSubscriptionNumber() { return subscriptionNumber; }
    public void setSubscriptionNumber(String subscriptionNumber) { this.subscriptionNumber = subscriptionNumber; }

    public BigDecimal getMonthlyDue() { return monthlyDue; }
    public void setMonthlyDue(BigDecimal monthlyDue) { this.monthlyDue = monthlyDue; }

    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public BigDecimal getTotalDue() { return totalDue; }
    public void setTotalDue(BigDecimal totalDue) { this.totalDue = totalDue; }

    public String getBillStatus() { return billStatus; }
    public void setBillStatus(String billStatus) { this.billStatus = billStatus; }
}
