package com.backend.water_management_system.settings.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.settings.dto.SystemDetailsRequest;
import com.backend.water_management_system.settings.dto.SystemDetailsResponse;
import com.backend.water_management_system.settings.service.SystemSettingsService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system-settings")
public class SystemSettingsController {
    
    private final SystemSettingsService systemSettingsService;

    @GetMapping("/get")
    public ResponseEntity<SystemDetailsResponse> getSystemDetails(){
        return ResponseEntity.ok(systemSettingsService.getSystemDetails());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/update")
    public ResponseEntity<SystemDetailsResponse> updateSystemDetails(@RequestBody SystemDetailsRequest request){
        return ResponseEntity.ok(systemSettingsService.updateSystemDetails(request));
    }
}
