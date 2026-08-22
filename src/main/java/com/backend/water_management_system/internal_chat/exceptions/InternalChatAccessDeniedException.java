package com.backend.water_management_system.internal_chat.exceptions;

/**
 * Signals that a user is not authorized to access a conversation or participant
 * record.
 */
public class InternalChatAccessDeniedException extends RuntimeException {
    /** Creates an authorization failure with a client-facing explanation. */
    public InternalChatAccessDeniedException(String message) {
        super(message);
    }
}
