package com.backend.water_management_system.dto;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import com.backend.water_management_system.entity.PaymentType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BankSlipUploadRequest {

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Bank reference is required")
    private String bankReference;

    @NotNull(message = "Bank slip file is required")
    private MultipartFile file;

}
