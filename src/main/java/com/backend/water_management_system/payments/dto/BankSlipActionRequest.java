package com.backend.water_management_system.payments.dto;

import com.backend.water_management_system.payments.enums.SlipStatus;

import lombok.Data;

@Data
public class BankSlipActionRequest {
    private Long slipId;
    private SlipStatus action; // APPROVE or REJECT
    private String rejectionReason; // Optional, only needed if action is REJECT
}
