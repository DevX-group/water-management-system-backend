package com.backend.water_management_system.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.backend.water_management_system.entity.Region;
import com.backend.water_management_system.repository.BillRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.RegionRepository;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Bill;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final RegionRepository regionRepository;
    private final BillRepository billRepository;

    public DataSeeder(CustomerRepository customerRepository, RegionRepository regionRepository,
            BillRepository billRepository) {
        this.customerRepository = customerRepository;
        this.regionRepository = regionRepository;
        this.billRepository = billRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        if (customerRepository.count() > 0) {
            return;
        }

        Region northRegion = new Region("R001", "north");
        Region southRegion = new Region("R002", "south");
        Region eastRegion = new Region("R003", "east");
        Region westRegion = new Region("R004", "west");

        regionRepository.save(northRegion);
        regionRepository.save(southRegion);
        regionRepository.save(eastRegion);
        regionRepository.save(westRegion);

        Customer c1 = new Customer("SK-2341", "Sanjeewa Kumara", northRegion, new BigDecimal("0.00"));
        Customer c2 = new Customer("SP-4589", "Supun Perera", southRegion, new BigDecimal("500.00"));
        Customer c3 = new Customer("KS-7892", "Kamani Silva", northRegion, new BigDecimal("1200.00"));
        Customer c4 = new Customer("RJ-1234", "Ruwan Jayawardena", eastRegion, new BigDecimal("2750.00"));
        Customer c5 = new Customer("PD-5678", "Priyantha De Silva", westRegion, new BigDecimal("0.00"));

        customerRepository.save(c1);
        customerRepository.save(c2);
        customerRepository.save(c3);
        customerRepository.save(c4);
        customerRepository.save(c5);

        // Sanjeewa Kumara
        Bill b1 = new Bill();
        b1.setCustomer(c1);
        b1.setBillingPeriod("2026-02");
        b1.setBillDate(LocalDate.now());
        b1.setDueDate(LocalDate.now().plusDays(14));
        b1.setUsageUnits(150);
        b1.setBaseCharge(new BigDecimal("50.00"));
        b1.setUsageCharge(new BigDecimal("1800.00"));
        b1.setTaxAmount(BigDecimal.ZERO);
        b1.setTotalAmount(new BigDecimal("1850.00"));
        b1.setBalanceDue(new BigDecimal("1850.00"));
        b1.setStatus("PENDING");
        b1.setGeneratedAt(OffsetDateTime.now());
        billRepository.save(b1);

        // Supun Perera
        Bill b2 = new Bill();
        b2.setCustomer(c2);
        b2.setBillingPeriod("2026-02");
        b2.setBillDate(LocalDate.now());
        b2.setDueDate(LocalDate.now().plusDays(14));
        b2.setUsageUnits(180);
        b2.setBaseCharge(new BigDecimal("50.00"));
        b2.setUsageCharge(new BigDecimal("2050.00"));
        b2.setTaxAmount(BigDecimal.ZERO);
        b2.setTotalAmount(new BigDecimal("2100.00"));
        b2.setBalanceDue(new BigDecimal("2100.00"));
        b2.setStatus("PENDING");
        b2.setGeneratedAt(OffsetDateTime.now());
        billRepository.save(b2);

        // Kamani Silva
        Bill b3 = new Bill();
        b3.setCustomer(c3);
        b3.setBillingPeriod("2026-02");
        b3.setBillDate(LocalDate.now());
        b3.setDueDate(LocalDate.now().plusDays(14));
        b3.setUsageUnits(20);
        b3.setBaseCharge(new BigDecimal("50.00"));
        b3.setUsageCharge(new BigDecimal("250.00"));
        b3.setTaxAmount(BigDecimal.ZERO);
        b3.setTotalAmount(new BigDecimal("300.00"));
        b3.setBalanceDue(new BigDecimal("300.00"));
        b3.setStatus("PENDING");
        b3.setGeneratedAt(OffsetDateTime.now());
        billRepository.save(b3);

        // Ruwan Jayawardena
        Bill b4 = new Bill();
        b4.setCustomer(c4);
        b4.setBillingPeriod("2026-02");
        b4.setBillDate(LocalDate.now());
        b4.setDueDate(LocalDate.now().plusDays(14));
        b4.setUsageUnits(300);
        b4.setBaseCharge(new BigDecimal("100.00"));
        b4.setUsageCharge(new BigDecimal("3400.00"));
        b4.setTaxAmount(BigDecimal.ZERO);
        b4.setTotalAmount(new BigDecimal("3500.00"));
        b4.setBalanceDue(new BigDecimal("3500.00"));
        b4.setStatus("PENDING");
        b4.setGeneratedAt(OffsetDateTime.now());
        billRepository.save(b4);

        // Priyantha De Silva
        Bill b5 = new Bill();
        b5.setCustomer(c5);
        b5.setBillingPeriod("2026-02");
        b5.setBillDate(LocalDate.now());
        b5.setDueDate(LocalDate.now().plusDays(14));
        b5.setUsageUnits(130);
        b5.setBaseCharge(new BigDecimal("50.00"));
        b5.setUsageCharge(new BigDecimal("1600.00"));
        b5.setTaxAmount(BigDecimal.ZERO);
        b5.setTotalAmount(new BigDecimal("1650.00"));
        b5.setBalanceDue(new BigDecimal("1650.00"));
        b5.setStatus("PENDING");
        b5.setGeneratedAt(OffsetDateTime.now());
        billRepository.save(b5);

        // Priyantha De Silva - Outstanding bill (older unpaid bill)

        Bill b5_old1 = new Bill();
        b5_old1.setCustomer(c5);
        b5_old1.setBillingPeriod("2026-01");
        b5_old1.setBillDate(LocalDate.now().minusMonths(1));
        b5_old1.setDueDate(LocalDate.now().minusMonths(1).plusDays(14));
        b5_old1.setUsageUnits(120);
        b5_old1.setBaseCharge(new BigDecimal("50.00"));
        b5_old1.setUsageCharge(new BigDecimal("1450.00"));
        b5_old1.setTaxAmount(BigDecimal.ZERO);
        b5_old1.setTotalAmount(new BigDecimal("1500.00"));
        b5_old1.setBalanceDue(new BigDecimal("950.00")); // unpaid remaining
        b5_old1.setStatus("PENDING");
        b5_old1.setGeneratedAt(OffsetDateTime.now().minusMonths(1));
        billRepository.save(b5_old1);

        // Supun Perera - Outstanding bills (older unpaid bills)

        // 2026-01 bill (partially unpaid)
        Bill b2_old1 = new Bill();
        b2_old1.setCustomer(c2);
        b2_old1.setBillingPeriod("2026-01");
        b2_old1.setBillDate(LocalDate.now().minusMonths(1));
        b2_old1.setDueDate(LocalDate.now().minusMonths(1).plusDays(14));
        b2_old1.setUsageUnits(160);
        b2_old1.setBaseCharge(new BigDecimal("50.00"));
        b2_old1.setUsageCharge(new BigDecimal("1850.00"));
        b2_old1.setTaxAmount(BigDecimal.ZERO);
        b2_old1.setTotalAmount(new BigDecimal("1900.00"));
        b2_old1.setBalanceDue(new BigDecimal("600.00")); // still unpaid
        b2_old1.setStatus("PENDING");
        b2_old1.setGeneratedAt(OffsetDateTime.now().minusMonths(1));
        billRepository.save(b2_old1);

        // 2025-12 bill (fully unpaid)
        Bill b2_old2 = new Bill();
        b2_old2.setCustomer(c2);
        b2_old2.setBillingPeriod("2025-12");
        b2_old2.setBillDate(LocalDate.now().minusMonths(2));
        b2_old2.setDueDate(LocalDate.now().minusMonths(2).plusDays(14));
        b2_old2.setUsageUnits(140);
        b2_old2.setBaseCharge(new BigDecimal("50.00"));
        b2_old2.setUsageCharge(new BigDecimal("1600.00"));
        b2_old2.setTaxAmount(BigDecimal.ZERO);
        b2_old2.setTotalAmount(new BigDecimal("1650.00"));
        b2_old2.setBalanceDue(new BigDecimal("1650.00")); // unpaid
        b2_old2.setStatus("PENDING");
        b2_old2.setGeneratedAt(OffsetDateTime.now().minusMonths(2));
        billRepository.save(b2_old2);
    }

}
