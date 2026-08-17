package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.CustomerPredictionResponse;
import com.backend.water_management_system.dto.MonthlyPredictionResponse;
import com.backend.water_management_system.service.PredictionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@CrossOrigin(origins = "http://localhost:8080")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/monthly")
    public List<MonthlyPredictionResponse> monthlyPrediction(
            @RequestParam int year
    ) {

        return predictionService.getMonthlyPrediction(year);
    }

    @GetMapping("/customer")
    public List<CustomerPredictionResponse> customerPrediction(
            @RequestParam String customerId,
            @RequestParam int year
    ) {
        return predictionService.getCustomerPrediction(customerId, year);
    }
}