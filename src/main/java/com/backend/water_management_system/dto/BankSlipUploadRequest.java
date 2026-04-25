package com.backend.water_management_system.dto;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import com.backend.water_management_system.entity.PaymentType;

import lombok.Data;

@Data
public class BankSlipUploadRequest {

    private String subscriptionNumber;
    private PaymentType paymentType;
    private BigDecimal amount;
    private String bankReference;
    private MultipartFile file;

}
