package com.backend.water_management_system.meter_reading.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.meter_reading.dto.MeterReadingCreateRequest;
import com.backend.water_management_system.meter_reading.dto.MeterReadingCreateResponse;
import com.backend.water_management_system.meter_reading.dto.MeterReadingTodayResponse;
import com.backend.water_management_system.meter_reading.service.MeterReadingService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/meter-readings")
@CrossOrigin
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN') or hasRole('METER_READER')")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    public MeterReadingController(MeterReadingService meterReadingService) {
        this.meterReadingService = meterReadingService;
    }

    @PostMapping                  // Submit a new meter reading and generate the corresponding bill
    public ResponseEntity<MeterReadingCreateResponse> submit(@RequestBody MeterReadingCreateRequest req) {
        Bill bill = meterReadingService.submitReadingAndGenerateBill(req);

        MeterReadingCreateResponse res = new MeterReadingCreateResponse();
        res.readingId = bill.getMeterReading().getReadingId();
        res.usageUnits = bill.getUsageUnits();
        res.billId = bill.getBillId();
        res.totalAmount = bill.getTotalAmount();
        res.status = bill.getStatus();

        return ResponseEntity.ok(res);
    }

    @GetMapping("/today")  // Get all meter readings submitted today
    public ResponseEntity<List<MeterReadingTodayResponse>> getTodaysReadings() {
        return ResponseEntity.ok(meterReadingService.getTodaysReadings());
    }

    @GetMapping("/previous/{meterNumber}")
    public ResponseEntity<MeterReadingTodayResponse> getPreviousReading(@PathVariable String meterNumber) {
        MeterReadingTodayResponse latest = meterReadingService.getLatestReadingByMeterNumber(meterNumber);
        if (latest != null) {
            return ResponseEntity.ok(latest);
        }
        return ResponseEntity.notFound().build();
    }
}
