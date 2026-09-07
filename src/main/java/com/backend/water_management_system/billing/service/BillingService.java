package com.backend.water_management_system.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import com.backend.water_management_system.alerts.service.AlertService;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.common.entity.ConnectionRate;
import com.backend.water_management_system.common.repository.RateRepository;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.meter_reading.entity.MeterReading;
import com.backend.water_management_system.notification.dto.NotificationRequest;
import com.backend.water_management_system.notification.enums.NotificationType;
import com.backend.water_management_system.notification.service.NotificationService;

@Service
public class BillingService {

    private final BillRepository billRepository;
    private final RateRepository rateRepository;
    private final NotificationService notificationService;
    private final AlertService alertService;

    public BillingService(BillRepository billRepository, RateRepository rateRepository,
            NotificationService notificationService, AlertService alertService) {
        this.billRepository = billRepository;
        this.rateRepository = rateRepository;
        this.notificationService = notificationService;
        this.alertService = alertService;
    }

    public Bill generateBill(Customer customer, MeterReading reading) {
        // 1. Get connection type
        final String type = (customer.getConnectionType() != null)
                ? customer.getConnectionType()
                : "metered";

        ConnectionRate rateEntity = rateRepository.findById(type)
                .orElseThrow(() -> new RuntimeException("Rates not found in DB for: " + type));

        int units = (reading.getUsageUnits() != null) ? reading.getUsageUnits() : 0;
        BigDecimal base = BigDecimal.valueOf(safeDouble(rateEntity.getBaseRate()));
        BigDecimal usageCharge = calculateTierUsageCharge(units, rateEntity);
        BigDecimal subtotal = base.add(usageCharge);
        BigDecimal taxRate = BigDecimal.valueOf(safeDouble(rateEntity.getTaxRate()));
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        BigDecimal outstandingAtIssue = billRepository.getTotalPendingBalance(customer.getSubscriptionNumber());
        System.out.println("CALCULATION: Type=" + type + " Units=" + units + " Base=" + base + " Total=" + total);

        Bill bill = new Bill();
        bill.setCustomer(customer);
        bill.setUsageUnits(units);
        bill.setBaseCharge(base);
        bill.setUsageCharge(usageCharge);
        bill.setTaxAmount(tax);
        bill.setTotalAmount(total);
        bill.setBalanceDue(total);
        bill.setStatus("PENDING");
        bill.setBillDate(LocalDate.now());
        bill.setDueDate(LocalDate.now().plusDays(30));
        bill.setBillingPeriod(YearMonth.now().toString());
        bill.setGeneratedAt(OffsetDateTime.now());
        bill.setMeterReading(reading);
        bill.setOutstandingAtIssue(outstandingAtIssue);
        billRepository.save(bill);

        String notificationMessage;

        if (outstandingAtIssue != null && outstandingAtIssue.compareTo(BigDecimal.ZERO) > 0) {
            notificationMessage = "Your new monthly water bill of Rs. "
                    + bill.getTotalAmount()
                    + " is now available. "
                    + "You also have an outstanding balance of Rs. "
                    + outstandingAtIssue
                    + ".";
        } else {
            notificationMessage = "Your new monthly water bill of Rs. "
                    + bill.getTotalAmount()
                    + " is now available.";
        }

        notificationService.sendNotification(
                NotificationRequest.builder()
                        .subscriptionNumber(customer.getSubscriptionNumber())
                        .notificationType(NotificationType.MONTHLY_BILL)
                        .title("New Monthly Bill")
                        .message(notificationMessage)
                        .build());
                        
        if (total.compareTo(new BigDecimal("5000")) > 0) {
            alertService.createAlert(
                "critical",
                "High Bill Alert",
                "Your recent water bill has exceeded 5000 LKR. Total: Rs. " + total,
                "N/A",
                customer.getSubscriptionNumber()
            );
        }

        return bill;
    }
    public Bill updateBill(Bill bill, MeterReading reading) {
        Customer customer = bill.getCustomer();
        final String type = (customer.getConnectionType() != null)
                ? customer.getConnectionType()
                : "metered";

        ConnectionRate rateEntity = rateRepository.findById(type)
                .orElseThrow(() -> new RuntimeException("Rates not found in DB for: " + type));

        int units = (reading.getUsageUnits() != null) ? reading.getUsageUnits() : 0;

        BigDecimal base = BigDecimal.valueOf(safeDouble(rateEntity.getBaseRate()));
        BigDecimal usageCharge = calculateTierUsageCharge(units, rateEntity);

        BigDecimal subtotal = base.add(usageCharge);
        BigDecimal taxRate = BigDecimal.valueOf(safeDouble(rateEntity.getTaxRate()));

        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        bill.setUsageUnits(units);
        bill.setBaseCharge(base);
        bill.setUsageCharge(usageCharge);
        bill.setTaxAmount(tax);
        bill.setTotalAmount(total);
        bill.setBalanceDue(total);
        bill.setMeterReading(reading);
        
        return billRepository.save(bill);
    }

    private BigDecimal calculateTierUsageCharge(int units, ConnectionRate rates) {
        if ("non_metered".equalsIgnoreCase(rates.getConnectionType())) {
            return BigDecimal.ZERO;
        }

        BigDecimal r1 = BigDecimal.valueOf(safeDouble(rates.getUnitRateTier1()));
        BigDecimal r2 = BigDecimal.valueOf(safeDouble(rates.getUnitRateTier2()));
        BigDecimal r3 = BigDecimal.valueOf(safeDouble(rates.getUnitRateTier3()));

        int limit1 = (rates.getTier1Limit() != null) ? rates.getTier1Limit() : 50;
        int limit2 = (rates.getTier2Limit() != null) ? rates.getTier2Limit() : 100;

        int tier1Units = Math.min(units, limit1);
        int tier2Units = Math.min(Math.max(units - limit1, 0), limit2 - limit1);
        int tier3Units = Math.max(units - limit2, 0);

        return r1.multiply(BigDecimal.valueOf(tier1Units))
                .add(r2.multiply(BigDecimal.valueOf(tier2Units)))
                .add(r3.multiply(BigDecimal.valueOf(tier3Units)));
    }
    private Double safeDouble(Double val) {
        return (val == null) ? 0.0 : val;
    }
}