package com.backend.water_management_system.messaging.exceptions;

public class MessagingNotFoundException extends RuntimeException {
    public MessagingNotFoundException(String message) {
        super(message);
    }
}
