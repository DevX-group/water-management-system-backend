package com.backend.water_management_system.payments.service;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.payments.dto.BankDetailsResponse;

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
