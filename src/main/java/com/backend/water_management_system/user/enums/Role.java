package com.backend.water_management_system.user.enums;

/*
  Security roles used for authentication and authorization only.
  These are NOT related to customer billing types (metered/non-metered).
 */
public enum Role {
    CUSTOMER,
    SUPER_ADMIN,
    SYSTEM_ADMIN,
    /**
     * CUSTOMER_HANDLER — was previously PAYMENT_HANDLER.
     * Handles billing, payments, customer management, and inquiries.
     */
    CUSTOMER_HANDLER,
    METER_READER
}
