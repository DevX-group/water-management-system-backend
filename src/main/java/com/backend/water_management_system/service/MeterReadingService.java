package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.MeterReadingCreateRequest;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.MeterReading;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.MeterReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final CustomerRepository customerRepository;
    private final BillingService billingService;

    public MeterReadingService(MeterReadingRepository meterReadingRepository,
                               CustomerRepository customerRepository,
                               BillingService billingService) {
        this.meterReadingRepository = meterReadingRepository;
        this.customerRepository = customerRepository;
        this.billingService = billingService;
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

        // generate bill mapped to this reading
        return billingService.generateBill(customer, savedReading);
    }
}

