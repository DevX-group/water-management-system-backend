package com.backend.water_management_system.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidPaymentException.class)
public ResponseEntity<String> handleInvalidPayment(InvalidPaymentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
}
}
