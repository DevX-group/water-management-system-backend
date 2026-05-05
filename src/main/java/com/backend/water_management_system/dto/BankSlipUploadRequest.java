package com.backend.water_management_system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.web.multipart.MultipartFile;

import com.backend.water_management_system.entity.PaymentType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BankSlipUploadRequest {

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Bank payment date is required")
    private LocalDate bankPaymentDate;

    @NotNull(message = "Bank reference is required")
    private String bankReference;

    @NotNull(message = "Bank slip file is required")
    private MultipartFile file;

}
