package com.backend.water_management_system.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="payment_allocations")
public class PaymentAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String paymentId;
    private Long billId;
    private BigDecimal amount;

    public String getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public Long getBillId() {
        return billId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
