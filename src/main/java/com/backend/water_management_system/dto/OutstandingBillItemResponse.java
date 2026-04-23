package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OutstandingBillItemResponse {
    private Long billId;
    private String billingPeriod;
    private LocalDate billDate;
    private BigDecimal balanceDue;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;

    public OutstandingBillItemResponse() {}

    public OutstandingBillItemResponse(Long billId, String billingPeriod, LocalDate billDate,
                                       BigDecimal balanceDue, String status, BigDecimal totalAmount, BigDecimal paidAmount) {
        this.billId = billId;
        this.billingPeriod = billingPeriod;
        this.billDate = billDate;
        this.balanceDue = balanceDue;
        this.status = status;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
    }

    
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getBillingPeriod() { return billingPeriod; }
    public void setBillingPeriod(String billingPeriod) { this.billingPeriod = billingPeriod; }
    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }
    public BigDecimal getBalanceDue() { return balanceDue; }
    public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
}
