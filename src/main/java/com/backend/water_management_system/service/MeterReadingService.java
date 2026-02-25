package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.MeterReadingCreateRequest;
import com.backend.water_management_system.dto.MeterReadingTodayResponse;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.MeterReading;
import com.backend.water_management_system.repository.BillRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.MeterReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final CustomerRepository customerRepository;
    private final BillingService billingService;
    private final BillRepository billRepository;

    public MeterReadingService(MeterReadingRepository meterReadingRepository,
                               CustomerRepository customerRepository,
                               BillingService billingService,
                               BillRepository billRepository) {
        this.meterReadingRepository = meterReadingRepository;
        this.customerRepository = customerRepository;
        this.billingService = billingService;
        this.billRepository = billRepository;
    }

    @Transactional
    public Bill submitReadingAndGenerateBill(MeterReadingCreateRequest req) {
        Customer customer = customerRepository.findById(req.subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + req.subscriptionNumber));

        if (req.currentReading < req.previousReading) {
            throw new RuntimeException("Current reading cannot be less than previous reading.");
        }

        int usage = req.currentReading - req.previousReading;

        MeterReading reading = new MeterReading();
        reading.setCustomer(customer);
        reading.setMeterNumber(req.meterNumber);
        reading.setPreviousReading(req.previousReading);
        reading.setCurrentReading(req.currentReading);
        reading.setUsageUnits(usage);
        reading.setReadingDate(req.readingDate);
        reading.setNotes(req.notes);
        reading.setSubmittedBy(req.submittedBy);

        MeterReading savedReading = meterReadingRepository.save(reading);

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
}