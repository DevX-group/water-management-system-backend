package com.backend.water_management_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.dto.BankSlipResponse;
import com.backend.water_management_system.dto.BankSlipUploadRequest;
import com.backend.water_management_system.service.BankSlipService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/api/slips")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class BankSlipController {

    private final BankSlipService bankSlipService;
       
    @PostMapping( value = "/upload", consumes = "multipart/form-data" )
    public ResponseEntity<BankSlipResponse> uploadSlip(@ModelAttribute BankSlipUploadRequest request) {
        System.out.println("File: " + request.getFile());
        return ResponseEntity.ok(
                bankSlipService.uploadSlip(request)
        );
    }
    
}
