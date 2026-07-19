package com.backend.water_management_system.messaging.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessagingSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(MessagingSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureTriggeredMessageTriggerTypesAreCurrent() {
        try {
            jdbcTemplate.execute("ALTER TABLE triggered_messages DROP CONSTRAINT IF EXISTS triggered_messages_trigger_type_check");
            jdbcTemplate.execute("ALTER TABLE triggered_messages ADD CONSTRAINT triggered_messages_trigger_type_check CHECK (trigger_type IN ('PAYMENT_CONFIRMED', 'BANK_SLIP_REJECTED', 'EMAIL_VERIFICATION', 'PHONE_VERIFICATION'))");
        } catch (Exception ex) {
            log.warn("Unable to refresh triggered_messages trigger_type constraint: {}", ex.getMessage());
        }
    }
}