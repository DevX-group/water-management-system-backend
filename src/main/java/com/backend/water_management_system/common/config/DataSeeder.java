package com.backend.water_management_system.common.config;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.common.entity.ConnectionRate;
import com.backend.water_management_system.common.entity.Region;
import com.backend.water_management_system.common.repository.RateRepository;
import com.backend.water_management_system.common.repository.RegionRepository;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
@Component
public class DataSeeder implements CommandLineRunner {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final BillRepository billRepository;
    private final RateRepository rateRepository;
    public DataSeeder(CustomerRepository customerRepository, UserRepository userRepository, RegionRepository regionRepository,
            BillRepository billRepository, RateRepository rateRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.billRepository = billRepository;
        this.rateRepository = rateRepository;
    }
    @Override
    public void run(String... args) throws Exception {
        if (rateRepository.count() == 0) {
            ConnectionRate meteredRate = new ConnectionRate();
            meteredRate.setConnectionType("metered");
            meteredRate.setBaseRate(50.0);
            meteredRate.setUnitRateTier1(10.0);
            meteredRate.setUnitRateTier2(15.0);
            meteredRate.setUnitRateTier3(20.0);
            meteredRate.setTier1Limit(50);
            meteredRate.setTier2Limit(100);
            meteredRate.setTaxRate(0.0); // or something
            rateRepository.save(meteredRate);
            ConnectionRate nonMeteredRate = new ConnectionRate();
            nonMeteredRate.setConnectionType("non_metered");
            nonMeteredRate.setBaseRate(500.0);
            nonMeteredRate.setUnitRateTier1(0.0);
            nonMeteredRate.setUnitRateTier2(0.0);
            nonMeteredRate.setUnitRateTier3(0.0);
            nonMeteredRate.setTier1Limit(0);
            nonMeteredRate.setTier2Limit(0);
            nonMeteredRate.setTaxRate(0.0);
            rateRepository.save(nonMeteredRate);
        }
        if (customerRepository.count() > 0) {
            return;
        }
        Region northRegion = new Region("R001", "north", true);
        Region southRegion = new Region("R002", "south", true);
        Region eastRegion = new Region("R003", "east", true);
        Region westRegion = new Region("R004", "west", true);
        Region centerRegion = new Region("R005", "center", true);
        regionRepository.save(northRegion);
        regionRepository.save(southRegion);
        regionRepository.save(eastRegion);
        regionRepository.save(westRegion);
        regionRepository.save(centerRegion);
        User u1 = User.builder().nic("921234567V").email("hansana47@gmail.com").phoneNumber("0711234567").role(Role.CUSTOMER).status(UserStatus.ACTIVE).build();
        userRepository.save(u1);
        Customer c1 = new Customer("SK-2341", "Hansana Thilakarathna", u1, "12 Lake Road, Colombo", "metered", northRegion);
        c1.setOutstandingBalance(new BigDecimal("0.00"));

        User u2 = User.builder().nic("881234568V").email("hanz4739@gmail.com").phoneNumber("0721234568").role(Role.CUSTOMER).status(UserStatus.ACTIVE).build();
        userRepository.save(u2);
        Customer c2 = new Customer("SP-4589", "Hansana Malshan", u2, "45 Temple Street, Galle", "metered", southRegion);
        c2.setOutstandingBalance(new BigDecimal("500.00"));

        User u3 = User.builder().nic("901234569V").email("kamani@example.com").phoneNumber("0771234569").role(Role.CUSTOMER).status(UserStatus.ACTIVE).build();
        userRepository.save(u3);
        Customer c3 = new Customer("KS-7892", "Kamani Silva", u3, "78 Main Street, Kandy", "non_metered", northRegion);
        c3.setOutstandingBalance(new BigDecimal("1200.00"));

        User u4 = User.builder().nic("851234570V").email("ruwan@example.com").phoneNumber("0751234570").role(Role.CUSTOMER).status(UserStatus.ACTIVE).build();
        userRepository.save(u4);
        Customer c4 = new Customer("RJ-1234", "Ruwan Jayawardena", u4, "101 Beach Road, Trincomalee", "metered", eastRegion);
        c4.setOutstandingBalance(new BigDecimal("2750.00"));

        User u5 = User.builder().nic("931234571V").email("priyantha@example.com").phoneNumber("0761234571").role(Role.CUSTOMER).status(UserStatus.ACTIVE).build();
        userRepository.save(u5);
        Customer c5 = new Customer("PD-5678", "Priyantha De Silva", u5, "22 Forest Avenue, Kurunegala", "non_metered", westRegion);
        c5.setOutstandingBalance(new BigDecimal("0.00"));
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
        b1.setOutstandingAtIssue(new BigDecimal("0.00"));
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
        b2.setOutstandingAtIssue(new BigDecimal("3550.00"));
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
        b3.setOutstandingAtIssue(new BigDecimal("0.00"));
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
        b4.setOutstandingAtIssue(new BigDecimal("0.00"));
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
        b5.setOutstandingAtIssue(new BigDecimal("1500.00"));
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
        b5_old1.setBalanceDue(new BigDecimal("1500.00"));
        b5_old1.setOutstandingAtIssue(new BigDecimal("0.00"));
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
        b2_old1.setBalanceDue(new BigDecimal("1900.00")); // still unpaid
        b2_old1.setStatus("PENDING");
        b2_old1.setGeneratedAt(OffsetDateTime.now().minusMonths(1));
        b2_old1.setOutstandingAtIssue(new BigDecimal("1650.00"));
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
        b2_old2.setOutstandingAtIssue(new BigDecimal("0.00"));
        billRepository.save(b2_old2);
    }
}
