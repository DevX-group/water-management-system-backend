package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.CustomerReportDTO;
import com.backend.water_management_system.repository.UsageRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerReportService {

    private final UsageRecordRepository repository;

    public CustomerReportService(UsageRecordRepository repository) {
        this.repository = repository;
    }

    public List<CustomerReportDTO> getCustomerReport(String customerId, int year) {

        List<Object[]> results = repository.getCustomerReport(customerId, year);

        return results.stream()
                .map(r -> new CustomerReportDTO(
                        (String) r[0],
                        ((Number) r[1]).doubleValue(),
                        ((Number) r[2]).doubleValue()
                ))
                .toList();
    }
}

