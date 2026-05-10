package com.backend.water_management_system.payments.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.dto.PaginationResponse;
import com.backend.water_management_system.payments.dto.AdminBankSlipResponse;
import com.backend.water_management_system.payments.dto.BankSlipActionRequest;
import com.backend.water_management_system.payments.dto.BankSlipUploadRequest;
import com.backend.water_management_system.payments.dto.BankSlipUploadResponse;
import com.backend.water_management_system.payments.dto.CustomerBankSlipResponse;
import com.backend.water_management_system.payments.service.BankSlipService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/slips")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8080")
public class BankSlipController {

    private final BankSlipService bankSlipService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<BankSlipUploadResponse> uploadSlip(@Valid @ModelAttribute BankSlipUploadRequest request) {
        return ResponseEntity.ok(
                bankSlipService.uploadSlip(request));
    }

    @GetMapping("/pending/all")
    public ResponseEntity<List<AdminBankSlipResponse>> getAllPendingSlips() {
        return ResponseEntity.ok(
                bankSlipService.getAllPendingSlips());
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<AdminBankSlipResponse>> getPendingSlips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(bankSlipService.getPendingSlips(page, size, search));
    }

    @DeleteMapping("/delete/{slipId}")
    public ResponseEntity<String> deleteSlip(@PathVariable Long slipId) {
        bankSlipService.deleteBankSlip(slipId);
        return ResponseEntity.ok("Bank slip deleted successfully.");
    }

    @PostMapping("/review")
    public ResponseEntity<String> processBankSlipReview(@RequestBody BankSlipActionRequest request) {
        bankSlipService.processBankSlipReview(request);
        return ResponseEntity.ok("Bank slip review processed successfully.");
    }

    @GetMapping("/my")
    public ResponseEntity<PaginationResponse<CustomerBankSlipResponse>> getMySlips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        return ResponseEntity.ok(bankSlipService.getBankSlipsBySubscriptionNumber(page, size));
    }

    @GetMapping("/{slipId}")
    public ResponseEntity<AdminBankSlipResponse> getBankSlipById(@PathVariable Long slipId) {
        return ResponseEntity.ok(bankSlipService.getBankSlipById(slipId));
    }
}
