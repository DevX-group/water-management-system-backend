package com.backend.water_management_system.payments.service;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.payments.dto.BankDetailsResponse;
import com.backend.water_management_system.settings.entity.SystemDetails;
import com.backend.water_management_system.settings.repository.SystemDetailsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentConfigService {
    private final SystemDetailsRepository systemDetailsRepository;
    
    public BankDetailsResponse getBankDetails() {
        SystemDetails details = systemDetailsRepository.findById(1L) 
            .orElseThrow(() -> new RuntimeException("System details not found")); 
            
        return new BankDetailsResponse( 
            details.getBankName(), 
            details.getBranch(), 
            details.getAccountNumber(), 
            details.getAccountName() 
        );
    }
}
