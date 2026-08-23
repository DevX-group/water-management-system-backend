-- Mock Data Seeder Script for Dashboard Widgets
-- Run this script in your PostgreSQL database (water_management_system) to populate data for widgets.

-- Fix for stale Hibernate check constraints on enums
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_type_check;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

-- 1. Meter Readings (Populates "System Usage Trend", "Today's Readings", "Reading History", and "Usage Trend" widgets)
-- Data for Customer R002-34844685
INSERT INTO meter_readings (subscription_number, meter_number, previous_reading, current_reading, usage_units, reading_date)
VALUES ('R002-34844685', 'MTR-1001', 1000, 1150, 150, CURRENT_DATE);

INSERT INTO meter_readings (subscription_number, meter_number, previous_reading, current_reading, usage_units, reading_date)
VALUES ('R002-34844685', 'MTR-1001', 850, 1000, 150, CURRENT_DATE - INTERVAL '1 month');

INSERT INTO meter_readings (subscription_number, meter_number, previous_reading, current_reading, usage_units, reading_date)
VALUES ('R002-34844685', 'MTR-1001', 710, 850, 140, CURRENT_DATE - INTERVAL '2 months');

-- Data for Customer SP-4589
INSERT INTO meter_readings (subscription_number, meter_number, previous_reading, current_reading, usage_units, reading_date)
VALUES ('SP-4589', 'MTR-1002', 2000, 2180, 180, CURRENT_DATE);

INSERT INTO meter_readings (subscription_number, meter_number, previous_reading, current_reading, usage_units, reading_date)
VALUES ('SP-4589', 'MTR-1002', 1830, 2000, 170, CURRENT_DATE - INTERVAL '1 month');


-- 2. Alerts (Populates "Active Alerts" widget)
-- Global alerts
INSERT INTO alert (severity, title, description, time, dismissed, customer_id, usage)
VALUES ('HIGH', 'High Usage Detected', 'Unusually high water usage in Region North.', CURRENT_TIMESTAMP, false, NULL, NULL);

INSERT INTO alert (severity, title, description, time, dismissed, customer_id, usage)
VALUES ('MEDIUM', 'System Maintenance', 'Scheduled maintenance tomorrow at 2 AM.', CURRENT_TIMESTAMP - INTERVAL '1 day', false, NULL, NULL);

-- Customer specific alert
INSERT INTO alert (severity, title, description, time, dismissed, customer_id, usage)
VALUES ('HIGH', 'Unusual Consumption', 'Your consumption is 50% higher than last month.', CURRENT_TIMESTAMP, false, 'SK-2341', '150');


-- 3. Notifications (Populates "Notifications" widget)
INSERT INTO notifications (subscription_number, title, message, read_status, created_at, notification_type)
VALUES ('R002-34844685', 'Bill Generated', 'Your monthly bill for this month is ready.', false, CURRENT_TIMESTAMP, 'MONTHLY_BILL');

INSERT INTO notifications (subscription_number, title, message, read_status, created_at, notification_type)
VALUES ('SP-4589', 'Payment Received', 'We have received your payment of Rs. 2100.', true, CURRENT_TIMESTAMP - INTERVAL '2 days', 'MANUAL_PAYMENT');

INSERT INTO notifications (subscription_number, title, message, read_status, created_at, notification_type)
VALUES ('R002-34844685', 'Bank Slip Approved', 'Your recent bank slip submission was approved.', false, CURRENT_TIMESTAMP, 'BANK_SLIP_APPROVED');


-- 4. Payments (Populates "Monthly Revenue" and "Recent Payments" widgets)
INSERT INTO payments (payment_id, subscription_number, amount, status, payment_type, payment_method, created_at)
VALUES (gen_random_uuid(), 'R002-34844685', 1500.00, 'FULL', 'MONTHLY', 'ONLINE', CURRENT_TIMESTAMP - INTERVAL '5 days');

INSERT INTO payments (payment_id, subscription_number, amount, status, payment_type, payment_method, created_at)
VALUES (gen_random_uuid(), 'SP-4589', 2100.00, 'PENDING', 'OUTSTANDING', 'BANK_TRANSFER', CURRENT_TIMESTAMP - INTERVAL '2 hours');

INSERT INTO payments (payment_id, subscription_number, amount, status, payment_type, payment_method, created_at)
VALUES (gen_random_uuid(), 'R002-34844685', 1450.00, 'FULL', 'MONTHLY', 'MANUAL', CURRENT_TIMESTAMP - INTERVAL '1 month 5 days');

INSERT INTO payments (payment_id, subscription_number, amount, status, payment_type, payment_method, created_at)
VALUES (gen_random_uuid(), 'SE-6720', 3200.00, 'FULL', 'MONTHLY', 'ONLINE', CURRENT_TIMESTAMP - INTERVAL '2 months 10 days');

-- 5. Usage Records (Populates "Monthly Revenue", "Reports", and "Predictions" widgets)
INSERT INTO usage_records (customer_id, area, usage, amount, record_date)
VALUES ('R002-34844685', 'North', 150, 1800.00, CURRENT_DATE);
INSERT INTO usage_records (customer_id, area, usage, amount, record_date)
VALUES ('R002-34844685', 'North', 140, 1650.00, CURRENT_DATE - INTERVAL '1 month');
INSERT INTO usage_records (customer_id, area, usage, amount, record_date)
VALUES ('R002-34844685', 'North', 160, 1950.00, CURRENT_DATE - INTERVAL '2 months');

INSERT INTO usage_records (customer_id, area, usage, amount, record_date)
VALUES ('SP-4589', 'South', 180, 2100.00, CURRENT_DATE);
INSERT INTO usage_records (customer_id, area, usage, amount, record_date)
VALUES ('SP-4589', 'South', 170, 1980.00, CURRENT_DATE - INTERVAL '1 month');

INSERT INTO usage_records (customer_id, area, usage, amount, record_date)
VALUES ('KS-7892', 'East', 200, 2400.00, CURRENT_DATE);
INSERT INTO usage_records (customer_id, area, usage, amount, record_date)
VALUES ('KS-7892', 'East', 190, 2250.00, CURRENT_DATE - INTERVAL '1 month');

