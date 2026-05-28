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
    public ResponseEntity<ApiError> handleMessagingNotFound(MessagingNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(ex.getMessage(), "MESSAGE_NOT_FOUND", 404));
    }

    @ExceptionHandler(MessagingValidationException.class)
    public ResponseEntity<ApiError> handleMessagingValidation(MessagingValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), "VALIDATION_ERROR", 400));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        "Something went wrong. Please try again.",
                        "INTERNAL_ERROR",
                        500
                ));
    }
}
