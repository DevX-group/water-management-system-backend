package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.MeterReadingCreateRequest;
import com.backend.water_management_system.dto.MeterReadingCreateResponse;
import com.backend.water_management_system.dto.MeterReadingTodayResponse;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.service.MeterReadingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meter-readings")
@CrossOrigin
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    public MeterReadingController(MeterReadingService meterReadingService) {
        this.meterReadingService = meterReadingService;
    }

    @PostMapping
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

    @GetMapping("/today")
    public ResponseEntity<List<MeterReadingTodayResponse>> getTodaysReadings() {
        return ResponseEntity.ok(meterReadingService.getTodaysReadings());
    }
}