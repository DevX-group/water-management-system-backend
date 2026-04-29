package com.backend.water_management_system.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.backend.water_management_system.dto.AdminBankSlipResponse;
import com.backend.water_management_system.dto.BankSlipActionRequest;
import com.backend.water_management_system.dto.BankSlipUploadResponse;
import com.backend.water_management_system.dto.CloudinaryUploadResponse;
import com.backend.water_management_system.dto.BankSlipUploadRequest;
import com.backend.water_management_system.dto.PaymentResult;
import com.backend.water_management_system.entity.BankSlip;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Payment;
import com.backend.water_management_system.entity.PaymentMethod;
import com.backend.water_management_system.entity.PaymentType;
import com.backend.water_management_system.entity.SlipStatus;
import com.backend.water_management_system.exception.BankSlipNotFoundException;
import com.backend.water_management_system.exception.BankSlipUploadException;
import com.backend.water_management_system.exception.InvalidPaymentException;
import com.backend.water_management_system.repository.BankSlipRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankSlipService {

        private final CloudinaryService cloudinaryService;
        private final PaymentService paymentService;
        private final BankSlipRepository bankSlipRepository;
        private final CustomerRepository customerRepository;
        private final PaymentRepository paymentRepository;

        private final SimpMessagingTemplate messagingTemplate;

        public BankSlipUploadResponse uploadSlip(BankSlipUploadRequest request) {
                MultipartFile file = request.getFile();

                if (file.getSize() > 5 * 1024 * 1024) {
                        throw new IllegalArgumentException("File size exceeds the maximum limit of 5MB");
                }

                String type = file.getContentType();

                if (type == null ||
                                (!type.equals("image/jpeg") &&
                                                !type.equals("image/png") &&
                                                !type.equals("application/pdf"))) {

                        throw new IllegalArgumentException("Only JPG, PNG, and PDF files are allowed");
                }

                boolean isDuplicateReference = bankSlipRepository.existsByBankReference(request.getBankReference());

                if (isDuplicateReference) {
                        throw new IllegalStateException("This bank reference number has already been used.");
                }

                String subscriptionNumber = "SK-2341"; // TODO: replace with JWT auth context

                String imageUrl = null;
                String publicId = null;

                try {
                        CloudinaryUploadResponse uploadResponse = cloudinaryService.uploadFile(file);
                        imageUrl = uploadResponse.getUrl();
                        publicId = uploadResponse.getPublicId();

                        String fileName = file.getOriginalFilename();
                        String fileType = file.getContentType();
                        Long fileSize = file.getSize();

                        BankSlip slip = BankSlip.builder()
                                        .subscriptionNumber(subscriptionNumber)
                                        .publicId(publicId)
                                        .amount(request.getAmount())
                                        .paymentType(request.getPaymentType())
                                        .bankReference(request.getBankReference())
                                        .filePath(imageUrl)
                                        .fileName(fileName)
                                        .fileType(fileType)
                                        .fileSize(fileSize)
                                        .status(SlipStatus.PENDING)
                                        .uploadedAt(LocalDateTime.now())
                                        .build();

                        BankSlip savedSlip = bankSlipRepository.save(slip);

                        AdminBankSlipResponse adminDto = mapToAdminDTO(savedSlip);

                        messagingTemplate.convertAndSend(
                                        "/topic/admin/bank-slips",
                                        adminDto);

                        return BankSlipUploadResponse.builder()
                                        .message("Bank slip uploaded successfully")
                                        .paymentType(request.getPaymentType())
                                        .amount(request.getAmount())
                                        .bankReference(savedSlip.getBankReference())
                                        .filePath(savedSlip.getFilePath())
                                        .status(savedSlip.getStatus())
                                        .uploadedAt(savedSlip.getUploadedAt())
                                        .build();

                } catch (Exception e) {
                        if(publicId != null){
                                 cloudinaryService.deleteFile(publicId); // Cleanup uploaded file on failure
                        }
                        throw new BankSlipUploadException("Failed to upload bank slip. Please try again.", e);
                }
        }

        public List<AdminBankSlipResponse> getPendingSlips() {
                List<BankSlip> pendingSlips = bankSlipRepository.findByStatus(SlipStatus.PENDING);

                return pendingSlips.stream()
                                .map(this::mapToAdminDTO)
                                .toList();
        }

        private AdminBankSlipResponse mapToAdminDTO(BankSlip slip) {

                String accountHolderName = customerRepository
                                .findBySubscriptionNumber(slip.getSubscriptionNumber())
                                .map(Customer::getAccountHolderName)
                                .orElse("Unknown");

                return AdminBankSlipResponse.builder()
                                .slipId(slip.getSlipId())
                                .subscriptionNumber(slip.getSubscriptionNumber())
                                .accountHolderName(accountHolderName)
                                .paymentType(slip.getPaymentType())
                                .amount(slip.getAmount())
                                .bankReference(slip.getBankReference())
                                .filePath(slip.getFilePath())
                                .status(slip.getStatus())
                                .uploadedAt(slip.getUploadedAt())
                                .build();
        }

        

        public void processBankSlipReview(BankSlipActionRequest request) {
                BankSlip slip = bankSlipRepository.findById(request.getBankSlipId())
                                .orElseThrow(() -> new BankSlipNotFoundException(
                                                "Bank slip not found with ID: " + request.getBankSlipId()));

                if (request.getAction() == SlipStatus.APPROVED) {
                        slip.setStatus(SlipStatus.APPROVED);
                        processApprovedBankSlips(slip);
                } else if (request.getAction() == SlipStatus.REJECTED) {
                        slip.setStatus(SlipStatus.REJECTED);
                } else {
                        throw new IllegalArgumentException("Invalid action. Must be APPROVE or REJECT.");
                }

                bankSlipRepository.save(slip);
        }

        public void processApprovedBankSlips(BankSlip slip) {
                String subscriptionNumber = slip.getSubscriptionNumber();
                BigDecimal amount = slip.getAmount();
                PaymentType paymentType = slip.getPaymentType();

                customerRepository.findById(subscriptionNumber)
                                .orElseThrow(() -> new RuntimeException(
                                                "Customer not found with subscription number: " + subscriptionNumber));

                Bill monthlyBill = null;
                List<Bill> outstandingBills = null;

                if (paymentType == PaymentType.MONTHLY) {
                        monthlyBill = paymentService.getLatestMonthlyBill(subscriptionNumber);
                        paymentService.validateMonthlyPayment(amount, monthlyBill);
                } else if (paymentType == PaymentType.OUTSTANDING) {
                        outstandingBills = paymentService.getOutstandingBillsEntites(subscriptionNumber);
                        paymentService.validateOutstandingPayment(amount, outstandingBills);
                }

                Payment payment = Payment.builder()
                                .paymentId(UUID.randomUUID().toString())
                                .subscriptionNumber(subscriptionNumber)
                                .amount(amount)
                                .paymentType(paymentType)
                                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                                .bankSlip(slip)
                                .build();

                PaymentResult result;

                if (paymentType == PaymentType.MONTHLY) {
                        result = paymentService.processMonthlyPayment(payment, amount, monthlyBill);
                } else if (paymentType == PaymentType.OUTSTANDING) {
                        result = paymentService.processOutstandingPayment(payment, amount, outstandingBills);
                } else {
                        throw new InvalidPaymentException("Unsupported payment type");
                }

                payment.setStatus(result.getStatus());
                paymentRepository.save(payment);

        }

}
