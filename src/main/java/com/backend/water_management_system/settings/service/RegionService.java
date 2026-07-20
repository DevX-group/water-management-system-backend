package com.backend.water_management_system.settings.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.common.entity.Region;
import com.backend.water_management_system.common.repository.RegionRepository;
import com.backend.water_management_system.settings.dto.AddRegionRequest;
import com.backend.water_management_system.settings.dto.AddRegionResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegionService {
    private final RegionRepository regionRepository;

    private String getLatestRegionCode() {
        Region region = regionRepository.findTopByOrderByRegionCodeDesc().orElse(null);
        return region != null ? region.getRegionCode() : null;
    }

    private String generateNextRegionCode() {
        String latestRegionCode = getLatestRegionCode();
        if (latestRegionCode == null) {
            return "R001"; // Starting code if no regions exist
        }
        // Extract the numeric part and increment it
        int lastNumber = Integer.parseInt(latestRegionCode.substring(1));
        return "R" + String.format("%03d", lastNumber + 1);
    }

    private AddRegionResponse mapToResponse(Region region) {
        return AddRegionResponse.builder()
                .regionCode(region.getRegionCode())
                .regionName(region.getRegionName())
                .build();
    }

    @Transactional
    public AddRegionResponse addRegion(AddRegionRequest request) {

        if (regionRepository.existsByRegionName(request.getRegionName())) {
            throw new IllegalArgumentException("Region name already exists.");
        }

        Region newRegion = new Region();

        newRegion.setRegionCode(generateNextRegionCode());
        newRegion.setRegionName(request.getRegionName().trim());
        newRegion.setActive(true);

        Region savedRegion = regionRepository.save(newRegion);

        return mapToResponse(savedRegion);
    }

    public List<Region> getAllActiveRegions() {
        return regionRepository.findByIsActiveTrue();
    }

    public void deleteRegion(String regionCode) {
        Region region = regionRepository.findById(regionCode)
                .orElseThrow(() -> new IllegalArgumentException("Region not found with code: " + regionCode));

        region.setActive(false);
        regionRepository.save(region);
    }
}
