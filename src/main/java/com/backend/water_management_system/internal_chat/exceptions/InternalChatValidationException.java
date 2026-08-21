package com.backend.water_management_system.internal_chat.exceptions;

/** Signals invalid internal-chat input or an ineligible conversation target. */
public class InternalChatValidationException extends RuntimeException {
    /** Creates a validation failure with a client-facing explanation. */
    public InternalChatValidationException(String message) {
        super(message);
    }
}
