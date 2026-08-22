package com.backend.water_management_system.meter_reading.service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.water_management_system.alerts.service.AlertService;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.billing.service.BillingService;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.meter_reading.dto.MeterReadingCreateRequest;
import com.backend.water_management_system.meter_reading.dto.MeterReadingTodayResponse;
import com.backend.water_management_system.meter_reading.entity.MeterReading;
import com.backend.water_management_system.meter_reading.repository.MeterReadingRepository;
@Service
public class MeterReadingService {
    private final MeterReadingRepository meterReadingRepository;
    private final CustomerRepository customerRepository;
    private final BillingService billingService;
    private final BillRepository billRepository;
    private final AlertService alertService;
    public MeterReadingService(MeterReadingRepository meterReadingRepository,
                               CustomerRepository customerRepository,
                               BillingService billingService,
                               BillRepository billRepository,
                               AlertService alertService) {
        this.meterReadingRepository = meterReadingRepository;
        this.customerRepository = customerRepository;
        this.billingService = billingService;
        this.billRepository = billRepository;
        this.alertService = alertService;
    }
    @Transactional
    public Bill submitReadingAndGenerateBill(MeterReadingCreateRequest req) {   //submitting a meter reading and generating a bill
        Customer customer = customerRepository.findById(req.subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + req.subscriptionNumber));
        int usage = 0;
        if (req.usageUnits != null) {
            usage = req.usageUnits;
        } else if (req.currentReading != null && req.previousReading != null) {
            if (req.currentReading < req.previousReading) {
                throw new RuntimeException("Current reading cannot be less than previous reading.");
            }
            usage = req.currentReading - req.previousReading;
        } else {
            throw new RuntimeException("Must provide either usageUnits or both current/previous readings.");
        }
        MeterReading reading = new MeterReading();
        reading.setCustomer(customer);
        reading.setMeterNumber(req.meterNumber);
        reading.setPreviousReading(req.previousReading);
        reading.setCurrentReading(req.currentReading);
        reading.setUsageUnits(usage);
        reading.setReadingDate(req.readingDate);
        reading.setImageUrl(req.imageUrl);
        reading.setNotes(req.notes);
        reading.setSubmittedBy(req.submittedBy);
        MeterReading savedReading = meterReadingRepository.save(reading);

        // Check for high usage (e.g., > 100 units) and create an alert
        if (usage > 100) {
            alertService.createAlert(
                "high",
                "High Water Usage Detected",
                "High usage detection",
                usage + " Units",
                customer.getSubscriptionNumber()
            );
        } else {
            // Normal reading alert
            alertService.createAlert(
                "info",
                "Meter Reading Submitted",
                "A normal meter reading was submitted successfully.",
                usage + " Units",
                customer.getSubscriptionNumber()
            );
        }

        return billingService.generateBill(customer, savedReading);
    }
    public List<MeterReadingTodayResponse> getTodaysReadings() {
        List<MeterReading> readings = meterReadingRepository.findByReadingDate(LocalDate.now());
        return readings.stream().map(r -> {
            MeterReadingTodayResponse dto = new MeterReadingTodayResponse();
            dto.readingId = r.getReadingId();
            dto.meterNumber = r.getMeterNumber();
            dto.previousReading = r.getPreviousReading();
            dto.currentReading = r.getCurrentReading();
            dto.usageUnits = r.getUsageUnits();
            dto.readingDate = r.getReadingDate();
            dto.imageUrl = r.getImageUrl();
            if (r.getCustomer() != null) {
                dto.customerName = r.getCustomer().getAccountHolderName();
                dto.subscriptionNumber = r.getCustomer().getSubscriptionNumber();
            }
            // Find linked bill
            Optional<Bill> bill = billRepository.findByMeterReading(r);
            bill.ifPresent(b -> {
                dto.billId = b.getBillId();
                dto.totalAmount = b.getTotalAmount();
                dto.billStatus = b.getStatus();
            });
            return dto;
        }).collect(Collectors.toList());
    }

    public MeterReadingTodayResponse getLatestReadingByMeterNumber(String meterNumber) {
        Optional<MeterReading> reading = meterReadingRepository.findTopByMeterNumberOrderByReadingDateDesc(meterNumber);
        if (reading.isPresent()) {
            MeterReading r = reading.get();
            MeterReadingTodayResponse dto = new MeterReadingTodayResponse();
            dto.readingId = r.getReadingId();
            dto.meterNumber = r.getMeterNumber();
            dto.previousReading = r.getPreviousReading();
            dto.currentReading = r.getCurrentReading();
            dto.usageUnits = r.getUsageUnits();
            dto.readingDate = r.getReadingDate();
            dto.imageUrl = r.getImageUrl();
            if (r.getCustomer() != null) {
                dto.customerName = r.getCustomer().getAccountHolderName();
                dto.subscriptionNumber = r.getCustomer().getSubscriptionNumber();
            }
            return dto;
        }
        return null;
    }
}