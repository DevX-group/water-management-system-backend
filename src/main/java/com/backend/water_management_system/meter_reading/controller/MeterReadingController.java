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

    @org.springframework.web.bind.annotation.PutMapping("/{readingId}")
    public ResponseEntity<MeterReadingCreateResponse> update(@PathVariable Long readingId, @RequestBody MeterReadingCreateRequest req) {
        Bill bill = meterReadingService.updateReading(readingId, req);

        MeterReadingCreateResponse res = new MeterReadingCreateResponse();
        res.readingId = bill.getMeterReading().getReadingId();
        res.usageUnits = bill.getUsageUnits();
        res.billId = bill.getBillId();
        res.totalAmount = bill.getTotalAmount();
        res.status = bill.getStatus();

        return ResponseEntity.ok(res);
    }

    @GetMapping("/today")  // Get all meter readings submitted for a specific date (defaults to today)
    public ResponseEntity<List<MeterReadingTodayResponse>> getTodaysReadings(
            @org.springframework.web.bind.annotation.RequestParam(required = false) 
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) 
            java.time.LocalDate date) {
        return ResponseEntity.ok(meterReadingService.getReadingsByDate(date));
    }

    @GetMapping("/previous/{meterNumber}")
    public ResponseEntity<MeterReadingTodayResponse> getPreviousReading(@PathVariable String meterNumber) {
        MeterReadingTodayResponse latest = meterReadingService.getLatestReadingByMeterNumber(meterNumber);
        if (latest != null) {
            return ResponseEntity.ok(latest);
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<java.util.Map<String, String>> uploadImage(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                                                     @org.springframework.beans.factory.annotation.Autowired com.backend.water_management_system.payments.service.CloudinaryService cloudinaryService) {
        if (cloudinaryService.isConfigured()) {
            com.backend.water_management_system.payments.dto.CloudinaryUploadResponse res = cloudinaryService.uploadFile(file);
            return ResponseEntity.ok(java.util.Map.of("url", res.getUrl()));
        } else {
            return ResponseEntity.ok(java.util.Map.of("url", "https://via.placeholder.com/600x400?text=Meter+Reading+Image"));
        }
    }
}
