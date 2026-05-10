package com.backend.water_management_system.payments.exceptions;

public class BankSlipUploadException extends RuntimeException {
     public BankSlipUploadException(String message) {
        super(message);
    }

    public BankSlipUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
