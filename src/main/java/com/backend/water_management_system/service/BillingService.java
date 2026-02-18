package com.backend.water_management_system.service;

import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.MeterReading;
import com.backend.water_management_system.entity.Region;
import com.backend.water_management_system.repository.BillRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;

@Service
public class BillingService {

    private final BillRepository billRepository;

    public BillingService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public Bill generateBill(Customer customer, MeterReading reading) {
        Region region = customer.getRegion();
        int units = reading.getUsageUnits() == null ? 0 : reading.getUsageUnits();

        BigDecimal base = region.getBaseRate() != null ? region.getBaseRate() : BigDecimal.ZERO;
        BigDecimal usageCharge = calculateTierUsageCharge(units, region);

        BigDecimal subtotal = base.add(usageCharge);
        BigDecimal taxRate = region.getTaxRate() != null ? region.getTaxRate() : BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(taxRate);

        BigDecimal total = subtotal.add(tax);

        Bill bill = new Bill();
        bill.setCustomer(customer);
        bill.setUsageUnits(units);
        bill.setBaseCharge(base);
        bill.setUsageCharge(usageCharge);
        bill.setTaxAmount(tax);
        bill.setTotalAmount(total);
        bill.setBalanceDue(total);
        bill.setStatus("PENDING");

        LocalDate billDate = reading.getReadingDate() != null ? reading.getReadingDate() : LocalDate.now();
        bill.setBillDate(billDate);
        bill.setDueDate(billDate.plusDays(30));

        YearMonth ym = YearMonth.from(billDate);
        bill.setBillingPeriod(ym.toString()); // "2026-02"

        bill.setGeneratedAt(OffsetDateTime.now());
        bill.setMeterReading(reading);

        return billRepository.save(bill);
    }

    // Example tier rules:
    // 0-50 => tier1
    // 51-100 => tier2
    // 101+ => tier3
    private BigDecimal calculateTierUsageCharge(int units, Region region) {
        BigDecimal t1 = region.getUnitRateTier1() != null ? region.getUnitRateTier1() : BigDecimal.ZERO;
        BigDecimal t2 = region.getUnitRateTier2() != null ? region.getUnitRateTier2() : BigDecimal.ZERO;
        BigDecimal t3 = region.getUnitRateTier3() != null ? region.getUnitRateTier3() : BigDecimal.ZERO;

        int tier1Units = Math.min(units, 50);
        int tier2Units = Math.min(Math.max(units - 50, 0), 50);
        int tier3Units = Math.max(units - 100, 0);

        return t1.multiply(BigDecimal.valueOf(tier1Units))
                .add(t2.multiply(BigDecimal.valueOf(tier2Units)))
                .add(t3.multiply(BigDecimal.valueOf(tier3Units)));
    }
}
