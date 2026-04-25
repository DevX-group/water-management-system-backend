package com.backend.water_management_system.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.backend.water_management_system.dto.BankSlipResponse;
import com.backend.water_management_system.dto.BankSlipUploadRequest;
import com.backend.water_management_system.entity.BankSlip;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.SlipStatus;
import com.backend.water_management_system.repository.BankSlipRepository;
import com.backend.water_management_system.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankSlipService {
    
    private final CloudinaryService cloudinaryService;
    private final BankSlipRepository bankSlipRepository;
    private final CustomerRepository customerRepository;

    public BankSlipResponse uploadSlip(BankSlipUploadRequest request) {
        MultipartFile file = request.getFile();
        String subscriptionNumber = request.getSubscriptionNumber();

        if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Bank slip file is required");
        }

        String imageUrl = cloudinaryService.uploadFile(file);
        
        String fileName = file.getOriginalFilename();
        String fileType = file.getContentType();
        Long fileSize = file.getSize();

        Customer customer = customerRepository.findBySubscriptionNumber(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found with subscription number: " + subscriptionNumber));

        BankSlip slip = BankSlip.builder()
                .subscriptionNumber(subscriptionNumber)
                .bankReference(request.getBankReference())
                .filePath(imageUrl)
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(fileSize)
                .status(SlipStatus.PENDING)
                .uploadedAt(LocalDateTime.now())
                .build();

        BankSlip savedSlip = bankSlipRepository.save(slip);

        return BankSlipResponse.builder()
                .slipId(savedSlip.getSlipId())
                .subscriptionNumber(savedSlip.getSubscriptionNumber())
                .accountHolderName(customer.getAccountHolderName())
                .paymentType(request.getPaymentType())
                .amount(request.getAmount())
                .bankReference(savedSlip.getBankReference())
                .filePath(savedSlip.getFilePath())
                .status(savedSlip.getStatus())
                .uploadedAt(savedSlip.getUploadedAt())
                .build();
    }

}
