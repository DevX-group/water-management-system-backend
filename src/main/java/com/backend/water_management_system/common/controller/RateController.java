package com.backend.water_management_system.common.controller;

import com.backend.water_management_system.common.entity.ConnectionRate;
import com.backend.water_management_system.common.repository.RateRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@CrossOrigin(origins = { "http://localhost:8080" })
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN') or hasRole('CUSTOMER_HANDLER') or hasRole('METER_READER')")
public class RateController {

    private final RateRepository rateRepository;

    public RateController(RateRepository rateRepository) {
        this.rateRepository = rateRepository;
    }

    @GetMapping
    public ResponseEntity<List<ConnectionRate>> getAllRates() {
        return ResponseEntity.ok(rateRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<ConnectionRate> saveRate(@RequestBody ConnectionRate rate) {
        ConnectionRate savedRate = rateRepository.save(rate);
        return ResponseEntity.ok(savedRate);
    }
}
