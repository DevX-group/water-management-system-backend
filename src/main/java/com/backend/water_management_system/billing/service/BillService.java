package com.backend.water_management_system.billing.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.billing.dto.BillResponse;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;

@Service
public class BillService {

    private final BillRepository billRepository;

    public BillService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public List<BillResponse> getBillsForCustomer(String subscriptionNumber) {    // Retrieve all bills for a specific customer
        return billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<BillResponse> searchBills(String query) {
        return billRepository.searchBills(query)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public org.springframework.data.domain.Page<BillResponse> getBillsForCustomerPaginated(String subscriptionNumber, org.springframework.data.domain.Pageable pageable) {
        return billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber, pageable)
                .map(this::toDto);
    }

    public Bill getBillEntityById(Long id) {       // Retrieve a Bill entity by its ID, used for generating PDF or image 
        return billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found: " + id));
    }


    public BillResponse getBillByShareToken(String shareToken) {
        return billRepository.findByShareToken(shareToken)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Invalid or expired bill link."));
    }

    private BillResponse toDto(Bill bill) {
        BillResponse dto = new BillResponse();
        dto.billId = bill.getBillId();
        dto.billingPeriod = bill.getBillingPeriod();
        dto.billDate = bill.getBillDate();
        dto.dueDate = bill.getDueDate();
        dto.usageUnits = bill.getUsageUnits();
        
        if (bill.getMeterReading() != null) {
            dto.previousReading = bill.getMeterReading().getPreviousReading();
            dto.currentReading = bill.getMeterReading().getCurrentReading();
        } else {
            dto.previousReading = 0;
            dto.currentReading = 0;
        }

        dto.baseCharge = bill.getBaseCharge();
        dto.usageCharge = bill.getUsageCharge();
        dto.taxAmount = bill.getTaxAmount();

        dto.totalAmount = bill.getTotalAmount();
        dto.balanceDue = bill.getBalanceDue();
        dto.status = bill.getStatus();
        dto.shareToken = bill.getShareToken();
        
        if (bill.getCustomer() != null) {
            dto.customerName = bill.getCustomer().getAccountHolderName();
            dto.nic = bill.getCustomer().getNic();
            dto.subscriptionNumber = bill.getCustomer().getSubscriptionNumber();
        }
        return dto;
    }
}

