package com.backend.water_management_system.service;

import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@NoArgsConstructor
public class MessagePlaceholderService {

    // Takes a template string and replaces placeholders with actual values from the
    // relevant customer and their bill.
    public String replacePlaceholders(String template, Customer customer, Bill currentBill) {
        if (template == null || template.isBlank()) {
            return "";
        }

        Map<String, String> values = new HashMap<>();
        values.put(MessagePlaceholder.CUSTOMER_NAME.getKey(),
                safe(customer != null ? customer.getAccountHolderName() : null));
        values.put(MessagePlaceholder.CUSTOMER_NUMBER.getKey(),
                safe(customer != null ? customer.getSubscriptionNumber() : null));
        values.put(MessagePlaceholder.OUTSTANDING_BALANCE.getKey(),
                formatNumber(customer != null ? customer.getOutstandingBalance() : null));

        values.put(MessagePlaceholder.BILLING_PERIOD.getKey(),
                safe(currentBill != null ? currentBill.getBillingPeriod() : null));
        values.put(MessagePlaceholder.BILL_DATE.getKey(),
                formatDate(currentBill != null ? currentBill.getBillDate() : null));
        values.put(MessagePlaceholder.BASE_CHARGE.getKey(),
                formatNumber(currentBill != null ? currentBill.getBaseCharge() : null));
        values.put(MessagePlaceholder.USAGE_UNITS.getKey(),
                formatInt(currentBill != null ? currentBill.getUsageUnits() : null));
        values.put(MessagePlaceholder.USAGE_CHARGE.getKey(),
                formatNumber(currentBill != null ? currentBill.getUsageCharge() : null));
        values.put(MessagePlaceholder.TAX_AMOUNT.getKey(),
                formatNumber(currentBill != null ? currentBill.getTaxAmount() : null));
        values.put(MessagePlaceholder.MONTHLY_FEE.getKey(),
                formatNumber(currentBill != null ? currentBill.getTotalAmount() : null));
        values.put(MessagePlaceholder.TOTAL_BALANCE.getKey(),
                formatNumber(currentBill != null ? currentBill.getBalanceDue() : null));
        values.put(MessagePlaceholder.DUE_DATE.getKey(),
                formatDate(currentBill != null ? currentBill.getDueDate() : null));

        values.put(MessagePlaceholder.OVERDUE_THRESHOLD.getKey(), "");
        values.put(MessagePlaceholder.RECONNECTION_FEE.getKey(), "");
        values.put(MessagePlaceholder.PRADESHIYA_SABHA_ACC_NO.getKey(), "");
        values.put(MessagePlaceholder.WHATSAPP_NUMBER.getKey(), "");
        values.put(MessagePlaceholder.ONLINE_BILL_PORTAL_LINK.getKey(), "");

        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String formatNumber(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String formatInt(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }
}
