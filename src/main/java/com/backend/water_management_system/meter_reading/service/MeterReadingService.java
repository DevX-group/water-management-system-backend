package com.backend.water_management_system.meter_reading.service;
import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.water_management_system.alerts.service.AlertService;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
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
    private final ActivityAuditService activityAuditService;
    public MeterReadingService(MeterReadingRepository meterReadingRepository,
                               CustomerRepository customerRepository,
                               BillingService billingService,
                               BillRepository billRepository,
                               AlertService alertService,
                               ActivityAuditService activityAuditService) {
        this.meterReadingRepository = meterReadingRepository;
        this.customerRepository = customerRepository;
        this.billingService = billingService;
        this.billRepository = billRepository;
        this.alertService = alertService;
        this.activityAuditService = activityAuditService;
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

        Bill bill = billingService.generateBill(customer, savedReading);
        activityAuditService.recordAuthenticatedWeb(
                AuditAction.METER_READING_CREATED,
                AuditEntityType.METER_READING,
                savedReading.getReadingId(),
                creationDetails(savedReading));
        return bill;
    }
    @Transactional
    public Bill updateReading(Long readingId, MeterReadingCreateRequest req) {
        MeterReading reading = meterReadingRepository.findById(readingId)
                .orElseThrow(() -> new RuntimeException("Reading not found"));

        Integer oldPreviousReading = reading.getPreviousReading();
        Integer oldCurrentReading = reading.getCurrentReading();
        Integer oldUsageUnits = reading.getUsageUnits();
        LocalDate oldReadingDate = reading.getReadingDate();
        
        int usage = 0;
        if (req.usageUnits != null) {
            usage = req.usageUnits;
        } else if (req.currentReading != null && req.previousReading != null) {
            if (req.currentReading < req.previousReading) {
                throw new RuntimeException("Current reading cannot be less than previous reading.");
            }
            usage = req.currentReading - req.previousReading;
        }

        reading.setPreviousReading(req.previousReading);
        reading.setCurrentReading(req.currentReading);
        reading.setUsageUnits(usage);
        reading.setReadingDate(req.readingDate);
        reading.setImageUrl(req.imageUrl);
        reading.setNotes(req.notes);
        
        MeterReading savedReading = meterReadingRepository.save(reading);
        
        Optional<Bill> existingBill = billRepository.findByMeterReading(savedReading);
        Bill bill;
        if (existingBill.isPresent()) {
            bill = billingService.updateBill(existingBill.get(), savedReading);
        } else {
            bill = billingService.generateBill(savedReading.getCustomer(), savedReading);
        }

        Map<String, String> changes = updateDetails(
                oldPreviousReading, savedReading.getPreviousReading(),
                oldCurrentReading, savedReading.getCurrentReading(),
                oldUsageUnits, savedReading.getUsageUnits(),
                oldReadingDate, savedReading.getReadingDate());
        if (!changes.isEmpty()) {
            activityAuditService.recordAuthenticatedWeb(
                    AuditAction.METER_READING_UPDATED,
                    AuditEntityType.METER_READING,
                    savedReading.getReadingId(),
                    changes);
        }
        return bill;
    }

    public List<MeterReadingTodayResponse> getReadingsByDate(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<MeterReading> readings = meterReadingRepository.findByReadingDate(targetDate);
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

    private static Map<String, String> creationDetails(MeterReading reading) {
        Map<String, String> details = new LinkedHashMap<>();
        if (reading.getPreviousReading() != null) {
            details.put("previousReading", reading.getPreviousReading().toString());
        }
        if (reading.getCurrentReading() != null) {
            details.put("currentReading", reading.getCurrentReading().toString());
        }
        if (reading.getUsageUnits() != null) {
            details.put("usageUnits", reading.getUsageUnits().toString());
        }
        if (reading.getReadingDate() != null) {
            details.put("readingDate", reading.getReadingDate().toString());
        }
        return details;
    }

    private static Map<String, String> updateDetails(
            Integer oldPrevious, Integer newPrevious,
            Integer oldCurrent, Integer newCurrent,
            Integer oldUsage, Integer newUsage,
            LocalDate oldDate, LocalDate newDate) {
        Map<String, String> details = new LinkedHashMap<>();
        addTransition(details, "previousReading", oldPrevious, newPrevious);
        addTransition(details, "currentReading", oldCurrent, newCurrent);
        addTransition(details, "usageUnits", oldUsage, newUsage);
        if (!Objects.equals(oldDate, newDate)) {
            if (oldDate != null && newDate != null) {
                details.put("readingDate", oldDate + " -> " + newDate);
            } else if (newDate != null) {
                details.put("readingDate", newDate.toString());
            } else if (oldDate != null) {
                details.put("readingDate", oldDate.toString());
            }
        }
        return details;
    }

    private static void addTransition(
            Map<String, String> details, String field, Integer oldValue, Integer newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            if (oldValue != null && newValue != null) {
                details.put(field, oldValue + " -> " + newValue);
            } else if (newValue != null) {
                details.put(field, newValue.toString());
            } else if (oldValue != null) {
                details.put(field, oldValue.toString());
            }
        }
    }
}
