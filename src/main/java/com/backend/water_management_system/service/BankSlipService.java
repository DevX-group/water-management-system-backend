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
import com.backend.water_management_system.dto.CustomerBankSlipResponse;
import com.backend.water_management_system.dto.BankSlipUploadRequest;
import com.backend.water_management_system.entity.BankSlip;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Payment;
import com.backend.water_management_system.entity.PaymentMethod;
import com.backend.water_management_system.entity.PaymentStatus;
import com.backend.water_management_system.entity.SlipStatus;
import com.backend.water_management_system.exception.BankSlipNotFoundException;
import com.backend.water_management_system.exception.BankSlipUploadException;
import com.backend.water_management_system.repository.BankSlipRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankSlipService {

        private final CloudinaryService cloudinaryService;
        private final CustomerPaymentService customerPaymentService;
        private final BankSlipRepository bankSlipRepository;
        private final CustomerRepository customerRepository;
        private final PaymentRepository paymentRepository;

        private final SimpMessagingTemplate messagingTemplate;

        // Business logic for handling bank slip uploads, including file validation,
        // duplicate reference checks, saving to database, and notifying admins via
        // WebSocket
        public BankSlipUploadResponse uploadSlip(BankSlipUploadRequest request) {
                MultipartFile file = request.getFile();

                // Validate file size and type before proceeding with upload
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

                // Check for duplicate bank reference
                boolean isDuplicateReference = bankSlipRepository.existsByBankReference(request.getBankReference());

                if (isDuplicateReference) {
                        throw new IllegalStateException("This bank reference number has already been used.");
                }

                String subscriptionNumber = "SK-2341"; // TODO: replace with JWT auth context

                String imageUrl = null;
                String publicId = null;

                try {
                        // Upload file to Cloudinary and get the URL and public ID for storage
                        CloudinaryUploadResponse uploadResponse = cloudinaryService.uploadFile(file);
                        imageUrl = uploadResponse.getUrl();
                        publicId = uploadResponse.getPublicId();

                        // Create and save the bank slip record in the database with all relevant
                        // details
                        String fileName = file.getOriginalFilename();
                        String fileType = file.getContentType();
                        Long fileSize = file.getSize();

                        BankSlip slip = BankSlip.builder()
                                        .subscriptionNumber(subscriptionNumber)
                                        .publicId(publicId)
                                        .amount(request.getAmount())
                                        .bankReference(request.getBankReference())
                                        .filePath(imageUrl)
                                        .fileName(fileName)
                                        .fileType(fileType)
                                        .fileSize(fileSize)
                                        .bankPaymentDate(request.getBankPaymentDate())
                                        .status(SlipStatus.PENDING)
                                        .uploadedAt(LocalDateTime.now())
                                        .build();

                        BankSlip savedSlip = bankSlipRepository.save(slip);

                        // Map the saved bank slip to an admin response DTO and send a WebSocket
                        // notification to admins about the new pending slip
                        AdminBankSlipResponse adminDto = mapToAdminDTO(savedSlip);

                        messagingTemplate.convertAndSend(
                                        "/topic/admin/bank-slips",
                                        adminDto);

                        // Return a response DTO to the customer with details of the uploaded slip
                        return BankSlipUploadResponse.builder()
                                        .message("Bank slip uploaded successfully")
                                        .slipId(savedSlip.getSlipId())
                                        .amount(request.getAmount())
                                        .bankReference(savedSlip.getBankReference())
                                        .filePath(savedSlip.getFilePath())
                                        .status(savedSlip.getStatus())
                                        .bankPaymentDate(savedSlip.getBankPaymentDate())
                                        .uploadedAt(savedSlip.getUploadedAt())
                                        .build();

                } catch (Exception e) {
                        if (publicId != null) {
                                cloudinaryService.deleteFile(publicId); // Cleanup uploaded file on failure
                        }
                        throw new BankSlipUploadException("Failed to upload bank slip. Please try again.", e);
                }
        }

        // Retrieves all pending bank slips from the database, maps them to admin
        // response DTOs, and returns the list for display in the admin payment review
        // interface
        public List<AdminBankSlipResponse> getPendingSlips() {
                List<BankSlip> pendingSlips = bankSlipRepository.findByStatus(SlipStatus.PENDING);

                return pendingSlips.stream()
                                .map(this::mapToAdminDTO)
                                .toList();
        }

        // Utility method to map a BankSlip entity to an AdminBankSlipResponse DTO,
        // including fetching the account holder name from the customer repository based
        // on the subscription number
        public AdminBankSlipResponse mapToAdminDTO(BankSlip slip) {

                String accountHolderName = customerRepository
                                .findBySubscriptionNumber(slip.getSubscriptionNumber())
                                .map(Customer::getAccountHolderName)
                                .orElse("Unknown");

                return AdminBankSlipResponse.builder()
                                .slipId(slip.getSlipId())
                                .subscriptionNumber(slip.getSubscriptionNumber())
                                .accountHolderName(accountHolderName)
                                .amount(slip.getAmount())
                                .bankReference(slip.getBankReference())
                                .filePath(slip.getFilePath())
                                .status(slip.getStatus())
                                .bankPaymentDate(slip.getBankPaymentDate())
                                .uploadedAt(slip.getUploadedAt())
                                .build();
        }

        // Deletes a bank slip
        @Transactional
        public void deleteBankSlip(Long slipId) {
                BankSlip slip = bankSlipRepository.findById(slipId)
                                .orElseThrow(() -> new BankSlipNotFoundException(
                                                "Bank slip not found with ID: " + slipId));

                String currentUser = "SK-2341"; // TODO: replace with JWT auth context

                if (!slip.getSubscriptionNumber().equals(currentUser)) {
                        throw new SecurityException("You do not have permission to delete this bank slip.");
                }

                if (slip.getStatus() != SlipStatus.PENDING) {
                        throw new IllegalStateException("Only pending bank slips can be deleted.");
                }

                cloudinaryService.deleteFile(slip.getPublicId());
                bankSlipRepository.delete(slip);
        }

        // Processes the review of a bank slip by an admin, updating the slip's status
        // to APPROVED or REJECTED based on the action specified in the request,
        // and if approved, triggers the payment processing logic to update the
        // customer's bills accordingly.
        // Also includes validation to ensure that a rejection reason is provided when
        // rejecting a slip.
        @Transactional
        public void processBankSlipReview(BankSlipActionRequest request) {
                if (request.getAction() == SlipStatus.REJECTED &&
                                (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {

                        throw new IllegalArgumentException("Rejection reason is required when rejecting a bank slip.");
                }

                BankSlip slip = bankSlipRepository.findById(request.getSlipId())
                                .orElseThrow(() -> new BankSlipNotFoundException(
                                                "Bank slip not found with ID: " + request.getSlipId()));

                if (slip.getStatus() != SlipStatus.PENDING) {
                        throw new IllegalStateException("This bank slip has already been reviewed.");
                }

                if (request.getAction() == SlipStatus.APPROVED) {
                        slip.setStatus(SlipStatus.APPROVED);
                        slip.setReviewedAt(LocalDateTime.now());
                        slip.setRejectionReason(null);
                        bankSlipRepository.save(slip);

                        processApprovedBankSlip(slip);
                } else if (request.getAction() == SlipStatus.REJECTED) {
                        slip.setStatus(SlipStatus.REJECTED);
                        slip.setRejectionReason(request.getRejectionReason());
                        slip.setReviewedAt(LocalDateTime.now());
                        bankSlipRepository.save(slip);
                } else {
                        throw new IllegalArgumentException("Invalid action. Must be APPROVE or REJECT.");
                }

        }

        // Core logic to handle the processing of an approved bank slip.
        public void processApprovedBankSlip(BankSlip slip) {
                String subscriptionNumber = slip.getSubscriptionNumber();
                BigDecimal amount = slip.getAmount();

                customerRepository.findBySubscriptionNumber(subscriptionNumber)
                                .orElseThrow(() -> new RuntimeException(
                                                "Customer not found with subscription number: " + subscriptionNumber));

                // Create a new payment record for the approved bank slip.
                Payment payment = Payment.builder()
                                .paymentId(UUID.randomUUID().toString())
                                .subscriptionNumber(subscriptionNumber)
                                .amount(amount)
                                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                                .bankSlip(slip)
                                .build();

                // Process the payment and update bills accordingly, determining final status
                // (FULL or PARTIAL)
                PaymentStatus status = customerPaymentService.processPayment(payment);

                payment.setStatus(status);
                paymentRepository.save(payment);

        }

        // Retrieves all bank slips associated with the currently authenticated
        // customer's subscription number.
        public List<CustomerBankSlipResponse> getBankSlipsBySubscriptionNumber() {
                String subscriptionNumber = "SK-2341"; // TODO: replace with JWT auth context
                return bankSlipRepository
                                .findBySubscriptionNumberOrderByUploadedAtDesc(subscriptionNumber)
                                .stream()
                                .map(this::mapToCustomerDTO)
                                .toList();
        }

        // Utility method to map a BankSlip entity to a CustomerBankSlipResponse DTO,
        // which is used to display the customer's own bank slips in their account
        // interface.
        private CustomerBankSlipResponse mapToCustomerDTO(BankSlip slip) {
                return CustomerBankSlipResponse.builder()
                                .slipId(slip.getSlipId())
                                .amount(slip.getAmount())
                                .bankReference(slip.getBankReference())
                                .filePath(slip.getFilePath())
                                .status(slip.getStatus())
                                .uploadedAt(slip.getUploadedAt())
                                .bankPaymentDate(slip.getBankPaymentDate())
                                .rejectionReason(slip.getRejectionReason())
                                .build();
        }

        // Retrieves a specific bank slip by its ID and maps it to an
        // AdminBankSlipResponse DTO for detailed viewing in the admin interface. This
        // method is used when an admin clicks on a specific slip to view its details
        // before approving or rejecting it.
        public AdminBankSlipResponse getBankSlipById(Long slipId) {
                BankSlip slip = bankSlipRepository.findById(slipId)
                                .orElseThrow(() -> new BankSlipNotFoundException(
                                                "Bank slip not found with ID: " + slipId));

                return mapToAdminDTO(slip);
        }

}
