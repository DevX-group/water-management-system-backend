package com.backend.water_management_system.predictions.controller;

import com.backend.water_management_system.predictions.dto.AreaPredictionResponse;
import com.backend.water_management_system.predictions.dto.CustomerPredictionResponse;
import com.backend.water_management_system.predictions.dto.MonthlyPredictionResponse;
import com.backend.water_management_system.predictions.service.PredictionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@CrossOrigin(origins = "*")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(
            PredictionService predictionService
    ) {
        this.predictionService = predictionService;
    }

    // Overall monthly prediction filtered by year
    @GetMapping("/monthly")
    public List<MonthlyPredictionResponse> monthlyPrediction(
            @RequestParam int year
    ) {
        return predictionService.getMonthlyPrediction(year);
    }

    // Customer prediction filtered by customer ID and year
    @GetMapping("/customer")
    public List<CustomerPredictionResponse> customerPrediction(
            @RequestParam String customerId,
            @RequestParam int year
    ) {
        return predictionService.getCustomerPrediction(
                customerId,
                year
        );
    }

    // Area prediction filtered by selected area and year
    @GetMapping("/area")
    public List<AreaPredictionResponse> areaPrediction(
            @RequestParam(defaultValue = "all") String area,
            @RequestParam int year
    ) {
        return predictionService.getAreaPrediction(
                area,
                year
        );
    }
}