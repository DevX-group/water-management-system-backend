package com.backend.water_management_system.dto;

import com.backend.water_management_system.entity.SlipStatus;

import lombok.Data;

@Data
public class BankSlipActionRequest {
    private Long bankSlipId;
    private SlipStatus action; // APPROVE or REJECT

}
