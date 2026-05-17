package com.backend.water_management_system.user.enums;

/**
 * Represents the lifecycle state of a user account.
 *
 * PENDING_ACTIVATION - Account created but password not yet set (awaiting activation link)
 * ACTIVE             - Account fully active, user can log in
 * INACTIVE           - Account deactivated by admin
 * SUSPENDED          - Account temporarily suspended
 */
public enum UserStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
