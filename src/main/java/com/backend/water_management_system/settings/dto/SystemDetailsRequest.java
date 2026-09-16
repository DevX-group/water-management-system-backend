package com.backend.water_management_system.settings.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemDetailsRequest {

    @Size(max = 100, message = "Company name must not exceed 100 characters")
    private String companyName;

    @Size(max = 255, message = "Office address must not exceed 255 characters")
    private String officeAddress;

    @Pattern(regexp = "^$|^[+0-9\\-\\s()]{7,20}$", message = "Invalid contact number format")
    private String officeContactNumber;

    @Email(message = "Office email must be a valid email address")
    @Size(max = 100, message = "Office email must not exceed 100 characters")
    private String officeEmail;

    @Size(max = 10, message = "Default currency must not exceed 10 characters")
    private String defaultCurrency;

    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @Size(max = 100, message = "Branch name must not exceed 100 characters")
    private String branch;

    @Pattern(regexp = "^[0-9]*$", message = "Account number must contain only numbers")
    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String accountNumber;

    @Size(max = 100, message = "Account name must not exceed 100 characters")
    private String accountName;

    @DecimalMin(value = "0.0", inclusive = true, message = "Overdue threshold must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 2, message = "Overdue threshold cannot have more than 2 decimal places")
    private BigDecimal overdueThreshold;

    @Min(value = 0, message = "Disconnection grace period must be greater than or equal to 0 days")
    @Max(value = 365, message = "Disconnection grace period must not exceed 365 days")
    private Integer disconnectionGracePeriodDays;

    @DecimalMin(value = "0.0", inclusive = true, message = "Reconnection fee must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 2, message = "Reconnection fee cannot have more than 2 decimal places")
    private BigDecimal reconnectionFee;
}
