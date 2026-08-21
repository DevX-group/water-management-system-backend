package com.backend.water_management_system.internal_chat.exceptions;

/**
 * Signals that a requested internal-chat user or conversation does not exist.
 */
public class InternalChatNotFoundException extends RuntimeException {
    /** Creates a not-found failure with a client-facing explanation. */
    public InternalChatNotFoundException(String message) {
        super(message);
    }
}
