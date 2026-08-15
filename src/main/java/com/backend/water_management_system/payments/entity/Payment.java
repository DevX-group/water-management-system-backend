package com.backend.water_management_system.payments.entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentStatus;
import com.backend.water_management_system.payments.enums.PaymentType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    private String paymentId;
    private String subscriptionNumber;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private LocalDateTime createdAt;

    //payhere data
    private String orderId;
    private String payherePaymentId;

    //bank slip data
    @OneToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "slip_id")
    private BankSlip bankSlip;

}
