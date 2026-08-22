package com.backend.water_management_system.reports.service;

import com.backend.water_management_system.reports.dto.AreaReportDTO;
import com.backend.water_management_system.reports.repository.UsageRecordRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AreaReportService {

    private final UsageRecordRepository repository;

    public AreaReportService(UsageRecordRepository repository) {
        this.repository = repository;
    }

    public List<AreaReportDTO> getAreaReport(
            int year,
            String area
    ) {
        String selectedArea =
                area == null || area.isBlank()
                        ? "all"
                        : area.trim().toLowerCase();

        List<Object[]> rows =
                repository.getAreaReport(year, selectedArea);

        Map<Integer, AreaReportDTO> monthlyReports =
                new LinkedHashMap<>();

        for (Object[] row : rows) {
            String month = (String) row[0];
            int monthNumber = ((Number) row[1]).intValue();
            String recordArea = (String) row[2];
            double usage = ((Number) row[3]).doubleValue();
            double revenue = ((Number) row[4]).doubleValue();

            AreaReportDTO report =
                    monthlyReports.computeIfAbsent(
                            monthNumber,
                            ignored -> new AreaReportDTO()
                    );

            report.setMonth(month);

            switch (recordArea.toLowerCase()) {
                case "area1" -> {
                    report.setArea1Usage(usage);
                    report.setArea1Revenue(revenue);
                }

                case "area2" -> {
                    report.setArea2Usage(usage);
                    report.setArea2Revenue(revenue);
                }

                case "area3" -> {
                    report.setArea3Usage(usage);
                    report.setArea3Revenue(revenue);
                }

                default -> {
                    // Ignore unknown area values.
                }
            }
        }

        return new ArrayList<>(monthlyReports.values());
    }
}