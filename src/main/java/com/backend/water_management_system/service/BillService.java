package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.BillResponse;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.repository.BillRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BillService {

    private final BillRepository billRepository;

    public BillService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public List<BillResponse> getBillsForCustomer(String subscriptionNumber) {
        return billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Bill getBillEntityById(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found: " + id));
    }


    private BillResponse toDto(Bill bill) {
        BillResponse dto = new BillResponse();
        dto.billId = bill.getBillId();
        dto.billingPeriod = bill.getBillingPeriod();
        dto.billDate = bill.getBillDate();
        dto.dueDate = bill.getDueDate();
        dto.usageUnits = bill.getUsageUnits();
        dto.totalAmount = bill.getTotalAmount();
        dto.balanceDue = bill.getBalanceDue();
        dto.status = bill.getStatus();
        return dto;
    }
}

