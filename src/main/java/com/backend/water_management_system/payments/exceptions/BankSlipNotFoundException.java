package com.backend.water_management_system.payments.exceptions;

public class BankSlipNotFoundException extends RuntimeException {
    public BankSlipNotFoundException(String message) {
        super(message);
    }

    public BankSlipNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
