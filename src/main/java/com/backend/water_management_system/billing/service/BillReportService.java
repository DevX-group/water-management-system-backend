package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.BillsSummaryDTO;
import com.backend.water_management_system.entity.BillReport;
import com.backend.water_management_system.repository.BillReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BillReportService {

    private final BillReportRepository repository;

    public BillReportService(BillReportRepository repository) {
        this.repository = repository;
    }

    // ALL bills
    public List<BillReport> getAllBills() {
        return repository.findAll();
    }

    // Bills by customer
    public List<BillReport> getBillsByCustomer(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    // ✅ ADD THIS → Overdue bills
    public List<BillReport> getOverdueBills() {
        LocalDate today = LocalDate.now();

        return repository.findAll().stream()
                .filter(bill ->
                        "UNPAID".equals(bill.getStatus()) &&
                                bill.getDueDate().isBefore(today)
                )
                .toList();
    }

    // Summary
    public BillsSummaryDTO getSummary(String customerId) {

        List<BillReport> bills = repository.findByCustomerId(customerId);

        double total = bills.stream()
                .mapToDouble(BillReport::getAmount)
                .sum();

        long unpaid = bills.stream()
                .filter(b -> "UNPAID".equals(b.getStatus()))
                .count();

        BillReport last = bills.stream()
                .reduce((a, b) -> a.getBillReportDate().isAfter(b.getBillReportDate()) ? a : b)
                .orElse(null);

        return new BillsSummaryDTO(
                customerId,
                bills.isEmpty() ? null : bills.get(0).getCustomerName(),
                total,
                last != null ? last.getBillReportDate() : null,
                unpaid
        );
    }
}