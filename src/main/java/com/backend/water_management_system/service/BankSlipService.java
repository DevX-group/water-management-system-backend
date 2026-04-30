package com.backend.water_management_system.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.backend.water_management_system.dto.BankSlipActionRequest;
import com.backend.water_management_system.dto.BankSlipResponse;
import com.backend.water_management_system.dto.BankSlipUploadRequest;
import com.backend.water_management_system.dto.PaymentResult;
import com.backend.water_management_system.entity.BankSlip;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Payment;
import com.backend.water_management_system.entity.PaymentMethod;
import com.backend.water_management_system.entity.PaymentType;
import com.backend.water_management_system.entity.SlipStatus;
import com.backend.water_management_system.exception.InvalidPaymentException;
import com.backend.water_management_system.repository.BankSlipRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankSlipService {

        private final CloudinaryService cloudinaryService;
        private final PaymentService paymentService;
        private final BankSlipRepository bankSlipRepository;
        private final CustomerRepository customerRepository;
        private final PaymentRepository paymentRepository;

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
                                .orElseThrow(() -> new RuntimeException(
                                                "Customer not found with subscription number: " + subscriptionNumber));

                BankSlip slip = BankSlip.builder()
                                .subscriptionNumber(subscriptionNumber)
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

        public void processBankSlipReview(BankSlipActionRequest request) {
                BankSlip slip = bankSlipRepository.findById(request.getBankSlipId())
                                .orElseThrow(() -> new RuntimeException(
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
