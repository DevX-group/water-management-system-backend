package com.backend.water_management_system.settings.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.common.entity.Region;
import com.backend.water_management_system.settings.dto.AddRegionRequest;
import com.backend.water_management_system.settings.dto.AddRegionResponse;
import com.backend.water_management_system.settings.service.RegionService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/regions")
@CrossOrigin(origins = "http://localhost:8080")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class RegionController {
    private final RegionService regionService;

    @PostMapping
    public ResponseEntity<AddRegionResponse> addRegion(@RequestBody AddRegionRequest request) {
        AddRegionResponse response = regionService.addRegion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<Region>> getAllActiveRegions() {
        List<Region> activeRegions = regionService.getAllActiveRegions();
        return ResponseEntity.ok(activeRegions);
    }

    @DeleteMapping("/delete/{regionCode}")
    public ResponseEntity<Void> deleteRegion(@PathVariable String regionCode) {
        regionService.deleteRegion(regionCode);
        return ResponseEntity.noContent().build();
    }
}
