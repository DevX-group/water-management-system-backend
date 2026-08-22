package com.backend.water_management_system.reports.service;

import com.backend.water_management_system.reports.dto.BillsSummaryDTO;
import com.backend.water_management_system.reports.entity.BillReport;
import com.backend.water_management_system.reports.repository.BillReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BillReportService {

    private final BillReportRepository repository;

    public BillReportService(BillReportRepository repository) {
        this.repository = repository;
    }

    // Returns all bills when customerId is empty.
    // Returns filtered bills when customerId is provided.
    public List<BillReport> getBills(String customerId) {
        String searchValue = normalizeSearchValue(customerId);

        return repository.findFilteredBills(searchValue);
    }

    // Exact lookup retained for existing endpoint
    public List<BillReport> getBillsByCustomer(
            String customerId
    ) {
        return repository.findByCustomerId(customerId.trim());
    }

    // Database performs overdue and customer filtering
    public List<BillReport> getOverdueBills(
            String customerId
    ) {
        String searchValue = normalizeSearchValue(customerId);

        return repository.findOverdueBills(
                LocalDate.now(),
                searchValue
        );
    }

    public BillsSummaryDTO getSummary(String customerId) {
        List<BillReport> bills =
                repository.findByCustomerId(customerId.trim());

        double totalAmount = bills.stream()
                .map(BillReport::getAmount)
                .filter(amount -> amount != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        long unpaidCount = bills.stream()
                .filter(bill ->
                        "UNPAID".equalsIgnoreCase(
                                bill.getStatus()
                        )
                )
                .count();

        BillReport latestBill = bills.stream()
                .filter(bill ->
                        bill.getBillReportDate() != null
                )
                .max((first, second) ->
                        first.getBillReportDate().compareTo(
                                second.getBillReportDate()
                        )
                )
                .orElse(null);

        String customerName = bills.stream()
                .map(BillReport::getCustomerName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);

        return new BillsSummaryDTO(
                customerId,
                customerName,
                totalAmount,
                latestBill == null
                        ? null
                        : latestBill.getBillReportDate(),
                unpaidCount
        );
    }

    private String normalizeSearchValue(String value) {
        return value == null ? "" : value.trim();
    }
}