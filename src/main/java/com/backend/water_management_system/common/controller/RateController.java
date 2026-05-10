package com.backend.water_management_system.common.controller;

import com.backend.water_management_system.common.entity.ConnectionRate;
import com.backend.water_management_system.common.repository.RateRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@CrossOrigin(origins = { "http://localhost:8080" })
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
