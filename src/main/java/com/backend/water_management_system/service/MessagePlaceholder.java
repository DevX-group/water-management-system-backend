package com.backend.water_management_system.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum MessagePlaceholder {
    CUSTOMER_NAME("customer_name"),
    CUSTOMER_NUMBER("customer_number"),
    BILLING_PERIOD("billing_period"),
    BILL_DATE("bill_date"),
    BASE_CHARGE("base_charge"),
    USAGE_UNITS("usage_units"),
    USAGE_CHARGE("usage_charge"),
    TAX_AMOUNT("tax_amount"),
    MONTHLY_FEE("monthly_fee"),
    OUTSTANDING_BALANCE("outstanding_balance"),
    TOTAL_BALANCE("total_balance"),
    DUE_DATE("due_date"),
    OVERDUE_THRESHOLD("overdue_threshold_(LKR)"),
    RECONNECTION_FEE("reconnection_fee_(LKR)"),
    PRADESHIYA_SABHA_ACC_NO("pradeshiya_sabha_acc_no"),
    WHATSAPP_NUMBER("whatsApp_number"),
    ONLINE_BILL_PORTAL_LINK("online_bill_portal_link");

    private final String key;

    MessagePlaceholder(String key) {
        this.key = key;
    }

    public static List<String> keys() {
        return Stream.of(values())
                .map(MessagePlaceholder::getKey)
                .collect(Collectors.toList());
    }
}
