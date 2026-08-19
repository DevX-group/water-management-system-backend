package com.backend.water_management_system.reports.service;

import com.backend.water_management_system.reports.dto.AreaReportDTO;
import com.backend.water_management_system.reports.repository.UsageRecordRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AreaReportService {

    private final UsageRecordRepository repository;

    public AreaReportService(UsageRecordRepository repository) {
        this.repository = repository;
    }

    public List<AreaReportDTO> getAreaReport(int year) {

        List<Object[]> rows = repository.getAreaReport(year);
        Map<Integer, AreaReportDTO> map = new LinkedHashMap<>();

        for (Object[] row : rows) {

            String month = (String) row[0];
            int monthNum = ((Number) row[1]).intValue();
            String area = (String) row[2];
            double usage = ((Number) row[3]).doubleValue();
            double amount = ((Number) row[4]).doubleValue();

            AreaReportDTO dto = map.getOrDefault(monthNum, new AreaReportDTO());
            dto.setMonth(month);

            switch (area.toLowerCase()) {
                case "area1":
                    dto.setArea1Usage(usage);
                    dto.setArea1Revenue(amount);
                    break;
                case "area2":
                    dto.setArea2Usage(usage);
                    dto.setArea2Revenue(amount);
                    break;
                case "area3":
                    dto.setArea3Usage(usage);
                    dto.setArea3Revenue(amount);
                    break;
            }

            map.put(monthNum, dto);
        }

        return new ArrayList<>(map.values());
    }
}
