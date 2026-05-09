package com.backend.water_management_system.service;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.dto.BankDetailsResponse;

@Service
public class PaymentConfigService {
    public BankDetailsResponse getBankDetails() {
        return new BankDetailsResponse(
                "Bank of Ceylon",
                "Colombo Main",
                "001-2031-4567",
                "NWSB – Water Services"
        );
    }
}
