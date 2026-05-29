package com.backend.water_management_system.billing.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.billing.dto.BillResponse;
import com.backend.water_management_system.billing.dto.CurrentBillResponse;
import com.backend.water_management_system.billing.dto.OutstandingBillsSummaryResponse;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.service.BillDocumentService;
import com.backend.water_management_system.billing.service.BillService;
import com.backend.water_management_system.payments.service.PaymentService;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = { "http://localhost:8080"})
public class BillController {

    private final BillService billService;
    private final PaymentService paymentService;
    private final BillDocumentService billDocumentService;

    public BillController(BillService billService, PaymentService paymentService, BillDocumentService billDocumentService) {
        this.billService = billService;
        this.paymentService = paymentService;
        this.billDocumentService = billDocumentService;
    }

    @GetMapping("/customer/{subscriptionNumber}")       // Get all bills for a specific customer by subscription number
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<BillResponse>> getCustomerBills(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(billService.getBillsForCustomer(subscriptionNumber));
    }

    @GetMapping("/current/{subscriptionNumber}")       // Get the current bill for a specific customer by subscription number
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<CurrentBillResponse> getCurrentBill(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(paymentService.getCurrentBill(subscriptionNumber));
    }

    @GetMapping("/outstanding/{subscriptionNumber}")      // Get a summary of outstanding bills 
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<OutstandingBillsSummaryResponse> getOutstandingBills(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(paymentService.getOutstandingBills(subscriptionNumber));
    }

    @GetMapping("/{billId}/download")            // Download the bill as a PDF document
    public ResponseEntity<byte[]> downloadBillPdf(@PathVariable Long billId) {
        try {
            Bill bill = billService.getBillEntityById(billId);
            byte[] pdfBytes = billDocumentService.generateBillPdf(bill);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "bill-" + bill.getBillingPeriod() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{billId}/image")          // Get the bill as an image (e.g., PNG) for display in the frontend
    public ResponseEntity<byte[]> getBillImage(@PathVariable Long billId) {
        try {
            Bill bill = billService.getBillEntityById(billId);
            byte[] imageBytes = billDocumentService.generateBillImage(bill);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}