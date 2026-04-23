package com.backend.water_management_system.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.dto.AlertResponse;
import com.backend.water_management_system.service.AlertService;

@RestController
@RequestMapping("/api/alert")
@CrossOrigin(origins = "*") 
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/customer/{subNum}")
    public List<AlertResponse> getAlerts(@PathVariable String subNum) {
        return alertService.getActiveAlerts(subNum);
    }

    @PutMapping("/{id}/dismiss")
    public void dismiss(@PathVariable Long id) {
        alertService.dismissAlert(id);
    }
}