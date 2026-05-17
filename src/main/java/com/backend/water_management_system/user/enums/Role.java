package com.backend.water_management_system.user.enums;

/**
 * Security roles used for authentication and authorization only.
 * These are NOT related to customer billing types (metered/non-metered).
 */
public enum Role {
    CUSTOMER,
    SUPER_ADMIN,
    SYSTEM_ADMIN,
    PAYMENT_HANDLER,
    METER_READER
}
