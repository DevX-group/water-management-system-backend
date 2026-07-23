package com.backend.water_management_system.settings.service;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.settings.dto.SystemDetailsRequest;
import com.backend.water_management_system.settings.dto.SystemDetailsResponse;
import com.backend.water_management_system.settings.entity.SystemDetails;
import com.backend.water_management_system.settings.repository.SystemDetailsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private final SystemDetailsRepository systemDetailsRepository;

    private SystemDetails findSystemDetails() {

        return systemDetailsRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("System details not initialized"));
    }

    private SystemDetailsResponse mapToResponse(SystemDetails details) {
        return SystemDetailsResponse.builder()
                .companyName(details.getCompanyName())
                .officeAddress(details.getOfficeAddress())
                .officeContactNumber(details.getOfficeContactNumber())
                .officeEmail(details.getOfficeEmail())
                .defaultCurrency(details.getDefaultCurrency())
                .bankName(details.getBankName())
                .branch(details.getBranch())
                .accountNumber(details.getAccountNumber())
                .accountName(details.getAccountName())
                .build();
    }

    private void updateIfNotBlank(String value, Consumer<String> setter) {
        if (value != null && !value.isBlank()) {
            setter.accept(value.trim());
        }
    }

    // Logic to update system details in the database
    public SystemDetailsResponse updateSystemDetails(SystemDetailsRequest request) {

        SystemDetails details = findSystemDetails();

        updateIfNotBlank(request.getCompanyName(), details::setCompanyName);
        updateIfNotBlank(request.getOfficeAddress(), details::setOfficeAddress);
        updateIfNotBlank(request.getOfficeContactNumber(), details::setOfficeContactNumber);
        updateIfNotBlank(request.getOfficeEmail(), details::setOfficeEmail);
        updateIfNotBlank(request.getDefaultCurrency(), details::setDefaultCurrency);

        updateIfNotBlank(request.getBankName(), details::setBankName);
        updateIfNotBlank(request.getBranch(), details::setBranch);
        updateIfNotBlank(request.getAccountNumber(), details::setAccountNumber);
        updateIfNotBlank(request.getAccountName(), details::setAccountName);

        details.setUpdatedAt(LocalDateTime.now());

        SystemDetails updatedDetails = systemDetailsRepository.save(details);

        return mapToResponse(updatedDetails);
    }

    // Logic to retrieve system details from the database
    public SystemDetailsResponse getSystemDetails() {

        SystemDetails details = findSystemDetails();

        return mapToResponse(details);
    }

}
