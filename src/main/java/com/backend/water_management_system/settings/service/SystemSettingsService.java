package com.backend.water_management_system.settings.service;

import java.time.LocalDateTime;

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
                .build();
    }

    // Logic to update system details in the database
    public SystemDetailsResponse updateSystemDetails(SystemDetailsRequest request) {

        SystemDetails details = findSystemDetails();

        if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            details.setCompanyName(request.getCompanyName().trim());
        }
        if (request.getOfficeAddress() != null && !request.getOfficeAddress().isBlank()) {
            details.setOfficeAddress(request.getOfficeAddress().trim());
        }
        if (request.getOfficeContactNumber() != null && !request.getOfficeContactNumber().isBlank()) {
            details.setOfficeContactNumber(request.getOfficeContactNumber().trim());
        }
        if (request.getOfficeEmail() != null && !request.getOfficeEmail().isBlank()) {
            details.setOfficeEmail(request.getOfficeEmail().trim());
        }
        if (request.getDefaultCurrency() != null && !request.getDefaultCurrency().isBlank()) {
            details.setDefaultCurrency(request.getDefaultCurrency().trim());
        }
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
