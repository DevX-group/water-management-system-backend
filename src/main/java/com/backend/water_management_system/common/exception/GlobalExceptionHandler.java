package com.backend.water_management_system.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.backend.water_management_system.messaging.exceptions.MessagingNotFoundException;
import com.backend.water_management_system.messaging.exceptions.MessagingValidationException;
import com.backend.water_management_system.payments.exceptions.InvalidPaymentException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPayment(InvalidPaymentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MessagingNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleMessagingNotFound(MessagingNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MessagingValidationException.class)
    public ResponseEntity<Map<String, String>> handleMessagingValidation(MessagingValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}
