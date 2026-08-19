package com.backend.water_management_system.reports.controller;

import com.backend.water_management_system.reports.dto.AreaReportDTO;
import com.backend.water_management_system.reports.service.AreaReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class AreaReportController {

    private final AreaReportService service;

    public AreaReportController(AreaReportService service) {
        this.service = service;
    }

    @GetMapping("/area")
    public List<AreaReportDTO> getAreaReport(@RequestParam int year) {
        return service.getAreaReport(year);
    }
}
