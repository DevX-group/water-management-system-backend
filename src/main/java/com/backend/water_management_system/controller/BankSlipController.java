package com.backend.water_management_system.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.dto.BankSlipUploadResponse;
import com.backend.water_management_system.dto.CustomerBankSlipResponse;
import com.backend.water_management_system.dto.AdminBankSlipResponse;
import com.backend.water_management_system.dto.BankSlipActionRequest;
import com.backend.water_management_system.dto.BankSlipUploadRequest;
import com.backend.water_management_system.service.BankSlipService;

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
       
    @PostMapping( value = "/upload", consumes = "multipart/form-data" )
    public ResponseEntity<BankSlipUploadResponse> uploadSlip(@Valid @ModelAttribute BankSlipUploadRequest request) {
        return ResponseEntity.ok(
                bankSlipService.uploadSlip(request)
        );
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AdminBankSlipResponse>> getPendingSlips() {
        return ResponseEntity.ok(
            bankSlipService.getPendingSlips()
        );
    }

    @DeleteMapping("/delete/{slipId}")
    public ResponseEntity<String> deleteSlip(@PathVariable Long slipId) {
        bankSlipService.deleteBankSlip(slipId);
        return ResponseEntity.ok("Bank slip deleted successfully.");
    }

    @PostMapping("/review")
    public ResponseEntity<String> processBankSlipReview(@RequestBody BankSlipActionRequest request){
        bankSlipService.processBankSlipReview(request);
        return ResponseEntity.ok("Bank slip review processed successfully.");
    }

    @GetMapping("/my")
    public ResponseEntity<List<CustomerBankSlipResponse>> getMySlips() {
        return ResponseEntity.ok(bankSlipService.getBankSlipsBySubscriptionNumber());
    }
}
