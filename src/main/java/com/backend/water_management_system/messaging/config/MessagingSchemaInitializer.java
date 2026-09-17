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
            // Legacy overdue-recipient messages are now handled conditionally by the
            // combined monthly bill message and must target all customers.
            jdbcTemplate.update(
                    "UPDATE scheduled_messages SET recipients = 'ALL_CUSTOMERS' WHERE recipients = 'OVERDUE_CUSTOMERS'");
            jdbcTemplate.update(
                    "UPDATE triggered_messages SET recipients = 'ALL_CUSTOMERS' WHERE recipients = 'OVERDUE_CUSTOMERS'");
            jdbcTemplate.update(
                    "UPDATE sent_messages SET recipients = 'ALL_CUSTOMERS' WHERE recipients = 'OVERDUE_CUSTOMERS'");
            jdbcTemplate.update(
                    "UPDATE message_channels SET channel = 'EMAIL' WHERE channel = 'Email'");

            repairCombinedScheduledMessage();
            repairCombinedTriggeredMessage();

            jdbcTemplate.execute(
                    "ALTER TABLE triggered_messages DROP CONSTRAINT IF EXISTS triggered_messages_trigger_type_check");
            jdbcTemplate.execute(
                    "ALTER TABLE triggered_messages ADD CONSTRAINT triggered_messages_trigger_type_check CHECK (trigger_type IN ('PAYMENT_CONFIRMED', 'BANK_SLIP_REJECTED', 'BILL_AND_OVERDUE', 'EMAIL_VERIFICATION', 'PHONE_VERIFICATION'))");
        } catch (Exception ex) {
            log.warn("Unable to refresh triggered_messages trigger_type constraint: {}", ex.getMessage());
        }
    }

    private void repairCombinedScheduledMessage() {
        int updated = jdbcTemplate.update("""
                UPDATE scheduled_messages
                SET is_default = true,
                    recipients = 'ALL_CUSTOMERS',
                    schedule_type = 'RECURRING',
                    schedule_day_of_month = COALESCE(schedule_day_of_month, 20),
                    schedule_time = COALESCE(schedule_time, TIME '08:00:00')
                WHERE name = 'Monthly Bill + Outstanding Alert'
                """);
        if (updated > 0) {
            addDefaultChannelsIfMissing("scheduled_messages");
        }
    }

    private void repairCombinedTriggeredMessage() {
        int updated = jdbcTemplate.update("""
                UPDATE triggered_messages
                SET is_default = true,
                    active = true,
                    recipients = 'ALL_CUSTOMERS',
                    trigger_type = 'BILL_AND_OVERDUE'
                WHERE name = 'Monthly Bill + Outstanding Alert'
                """);
        if (updated > 0) {
            addDefaultChannelsIfMissing("triggered_messages");
        }
    }

    private void addDefaultChannelsIfMissing(String messageTable) {
        jdbcTemplate.update("""
                INSERT INTO message_channels (message_id, channel)
                        SELECT message.id, channel_values.channel
                        FROM %s message
                        CROSS JOIN (VALUES ('SMS'), ('EMAIL')) AS channel_values(channel)
                WHERE name = 'Monthly Bill + Outstanding Alert'
                  AND NOT EXISTS (
                              SELECT 1 FROM message_channels mc
                              WHERE mc.message_id = message.id
                  )
                        """.formatted(messageTable));
    }
}