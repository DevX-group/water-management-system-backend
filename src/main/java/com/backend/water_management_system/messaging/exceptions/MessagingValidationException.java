package com.backend.water_management_system.messaging.exceptions;

public class MessagingValidationException extends RuntimeException {
    public MessagingValidationException(String message) {
        super(message);
    }
}
