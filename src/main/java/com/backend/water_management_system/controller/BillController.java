package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.BillResponse;
import com.backend.water_management_system.dto.CurrentBillResponse;
import com.backend.water_management_system.dto.OutstandingBillItemResponse;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.service.BillDocumentService;
import com.backend.water_management_system.service.BillService;
import com.backend.water_management_system.service.PaymentService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = {"http://localhost:3000" , "http://localhost:8080"})

public class BillController {

    private final BillService billService;
    private final PaymentService paymentService;
    private final BillDocumentService billDocumentService;

    public BillController(BillService billService, PaymentService paymentService, BillDocumentService billDocumentService) {
        this.billService = billService;
        this.paymentService = paymentService;
        this.billDocumentService = billDocumentService;
    }

    @GetMapping("/customer/{subscriptionNumber}")
    public ResponseEntity<List<BillResponse>> getCustomerBills(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(billService.getBillsForCustomer(subscriptionNumber));
    }

    @GetMapping("/current/{subscriptionNumber}")
    public ResponseEntity<CurrentBillResponse> getCurrentBill(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(paymentService.getCurrentBill(subscriptionNumber));
    }

    @GetMapping("/outstanding/{subscriptionNumber}")
    public ResponseEntity<List<OutstandingBillItemResponse>> getOutstandingBills(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(paymentService.getOutstandingBills(subscriptionNumber));
    }

    @GetMapping("/{billId}/download")
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

    @GetMapping("/{billId}/image")
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

