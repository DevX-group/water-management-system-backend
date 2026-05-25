package com.backend.water_management_system.payments.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.billing.dto.CurrentBillResponse;
import com.backend.water_management_system.billing.dto.OutstandingBillsSummaryResponse;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.common.dto.PaginationResponse;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.payments.exceptions.InvalidPaymentException;
import com.backend.water_management_system.payments.config.PayHereConfig;
import com.backend.water_management_system.payments.dto.CustomerAddPaymentRequest;
import com.backend.water_management_system.payments.dto.CustomerPaymentResponse;
import com.backend.water_management_system.payments.dto.PaymentHistoryItemResponse;
import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.enums.PaymentStatus;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.repository.PaymentRepository;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.messaging.service.TriggeredMessageDispatcher;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerPaymentService {

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PayHereConfig payHereConfig;
    private final BillRepository billRepository;
    private final TriggeredMessageDispatcher triggeredMessageDispatcher;
    private static final Logger log = LoggerFactory.getLogger(CustomerPaymentService.class);

    public CustomerPaymentResponse initiateCustomerPayment(CustomerAddPaymentRequest request) {

        if (request.getPaymentMethod() == null) {
            throw new InvalidPaymentException("Payment method is required");
        }

        BigDecimal amount = request.getAmount();
        String subscriptionNumber = "SK-2341"; // TODO: replace with JWT auth context

        BigDecimal totalBalance = billRepository.getTotalPendingBalance(subscriptionNumber);
        validateAmount(amount, totalBalance);

        // Create a new payment record with PENDING status before redirecting to PayHere
        Payment payment = createOnlinePayment(request, subscriptionNumber);

        // Prepare PayHere parameters and hash for redirect
        String orderId = "PAY-" + payment.getPaymentId();
        String merchantId = payHereConfig.getMerchantId();
        String merchantSecret = payHereConfig.getMerchantSecret();
        String currency = "LKR";
        String amountFormatted = amount.setScale(2, RoundingMode.HALF_UP).toString();
        String hash = getMd5(merchantId + orderId + amountFormatted + currency + getMd5(merchantSecret));

        Customer customer = customerRepository.findBySubscriptionNumber(subscriptionNumber)
                .orElseThrow(() -> new InvalidPaymentException(
                        "Customer with subscription number " + subscriptionNumber + " not found"));

        // Split account holder name into first and last name for PayHere parameters
        String accountHolderName = customer.getAccountHolderName();

        String[] parts = accountHolderName.trim().split("\\s+");

        String firstName;
        String lastName;

        if (parts.length == 1) {
            firstName = parts[0];
            lastName = "-";
        } else {
            firstName = parts[0];
            lastName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        }

        // Save payment before redirecting to PayHere (update it later in the
        // notification handler)
        payment.setOrderId(orderId);
        paymentRepository.save(payment);

        // Build response with all necessary parameters for frontend to redirect to
        // PayHere
        return CustomerPaymentResponse.builder()
                .orderId(orderId)
                .merchantId(merchantId)
                .items("Water Bill Payment")
                .firstName(firstName)
                .lastName(lastName)
                .email(customer.getEmail())
                .phoneNumber(customer.getMobileNumber())
                .address(customer.getAddress())
                .city(customer.getRegion().getRegionName())
                .country("Sri Lanka")
                .currency(currency)
                .amount(amount)
                .returnUrl(payHereConfig.getReturnUrl())
                .cancelUrl(payHereConfig.getCancelUrl())
                .notifyUrl(payHereConfig.getNotifyUrl())
                .hash(hash)
                .build();

    }

    // Validates that the payment amount is positive and does not exceed the total
    // balance due
    public void validateAmount(BigDecimal amount, BigDecimal totalBalance) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(totalBalance) > 0) {
            throw new InvalidPaymentException("Payment amount cannot exceed total balance due of " + totalBalance);
        }
    }

    // Creates a new Payment entity with PENDING status for an online payment before
    // redirecting to PayHere
    public Payment createOnlinePayment(CustomerAddPaymentRequest request, String subscriptionNumber) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setSubscriptionNumber(subscriptionNumber);
        payment.setAmount(request.getAmount());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(request.getPaymentMethod());
        return payment;
    }

    // Generates an MD5 hash of the input string, used for validating PayHere
    // notifications
    private String getMd5(String input) {
        if (input == null) {
            throw new IllegalArgumentException("MD5 input cannot be null — check all payment fields are populated");
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = String.format("%032x", no);
            return hashtext.toUpperCase();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    // Handles PayHere payment notifications, validating the data and updating
    // payment and bill records accordingly
    @Transactional
    public void handlePayhereNotification(java.util.Map<String, String> params) {

        log.info("params received = {}", params);

        // Clean and trim all parameters to prevent issues with whitespace or null
        // values
        Map<String, String> cleanParams = parseAndCleanParams(params);

        String orderId = cleanParams.get("order_id");
        String payherePaymentId = cleanParams.get("payment_id");
        String statusCode = cleanParams.get("status_code");
        String md5sig = cleanParams.get("md5sig");

        log.info("PayHere notify received. orderId={}, statusCode={}", orderId, statusCode);

        // Validate that all required parameters are present before proceeding
        validateBasicParams(orderId, payherePaymentId, statusCode, md5sig);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Payment not found for orderId={}", orderId);
                    return new InvalidPaymentException("Payment with order ID " + orderId + " not found");
                });

        String amountStr = cleanParams.get("payhere_amount");
        if (amountStr == null || amountStr.isBlank()) {
            amountStr = cleanParams.get("captured_amount");
        }
        BigDecimal amount = new BigDecimal(amountStr);

        // Validate that the amount in the notification matches the amount we expect for
        // this payment
        if (amount.compareTo(payment.getAmount()) != 0) {
            throw new InvalidPaymentException("Amount mismatch in PayHere notification for order ID " + orderId);
        }

        // Validating PayHere hash
        validateHash(amount, orderId, statusCode, md5sig);

        if ("2".equals(statusCode)) { // SUCCESS
            // already processed → do nothing
            if (payment.getStatus() == PaymentStatus.FULL
                    || payment.getStatus() == PaymentStatus.PARTIAL) {

                log.info("Payment already processed. orderId={}, status={}", orderId, payment.getStatus());

                // Optionally update PayHere ID if missing
                if (payment.getPayherePaymentId() == null) {
                    payment.setPayherePaymentId(payherePaymentId);
                    paymentRepository.save(payment);
                    log.info("Updated missing PayHere payment ID. orderId={}", orderId);
                }
                return;
            }

            log.info("Processing SUCCESS payment. orderId={}", orderId);

            // First-time success processing
            payment.setPayherePaymentId(payherePaymentId);

            // Process the payment and update bills accordingly, determining final status
            // (FULL or PARTIAL)
            PaymentStatus status = processPayment(payment);
            payment.setStatus(status);

            log.info("Payment processed successfully. orderId={}, finalStatus={}", orderId, status);

        } else { // FAILED or other statuses

            // Prevent downgrading a successful payment
            if (payment.getStatus() == PaymentStatus.FULL
                    || payment.getStatus() == PaymentStatus.PARTIAL) {
                return;
            }

            payment.setPayherePaymentId(payherePaymentId);

            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);

        log.info("Payment record saved. orderId={}, status={}", orderId, payment.getStatus());

        try {
            triggeredMessageDispatcher.dispatchPaymentConfirmed(payment);
        } catch (Exception ex) {
            log.warn("Failed to dispatch payment confirmation for {}: {}", payment.getPaymentId(), ex.getMessage());
        }
    }

    // Utility method to clean and trim all parameters from PayHere notification to
    // prevent issues with whitespace or null values
    private Map<String, String> parseAndCleanParams(Map<String, String> params) {
        Map<String, String> clean = new HashMap<>();

        params.forEach((k, v) -> {
            if (k != null) {
                clean.put(k.trim(), v == null ? "" : v.trim());
            }
        });

        return clean;
    }

    // Validates that all required parameters are present in the PayHere
    // notification before processing
    private void validateBasicParams(String orderId, String paymentId, String statusCode, String md5sig) {
        if (orderId == null || paymentId == null || statusCode == null || md5sig == null) {
            throw new InvalidPaymentException("Missing required PayHere parameters");
        }
    }

    // Validates the MD5 hash of the PayHere notification to ensure its integrity
    private void validateHash(BigDecimal amount, String orderId, String statusCode, String md5sig) {

        String amountFormatted = amount.setScale(2, RoundingMode.HALF_UP).toString();

        String merchantId = payHereConfig.getMerchantId();
        String merchantSecret = payHereConfig.getMerchantSecret();
        String currency = "LKR";

        log.info("Validating PayHere hash. orderId={}", orderId);

        String localHash = getMd5(
                merchantId + orderId + amountFormatted + currency + statusCode + getMd5(merchantSecret));

        if (!localHash.equalsIgnoreCase(md5sig)) {
            log.warn("Invalid PayHere hash. orderId={}", orderId);
            throw new InvalidPaymentException("Invalid MD5 signature in PayHere notification");
        }
    }

    // Core logic to allocate a payment amount to the customer's pending bills,
    // starting with the oldest, and updating bill statuses accordingly.
    public PaymentStatus processPayment(Payment payment) {

        List<Bill> bills = billRepository
                .findByCustomerSubscriptionNumberAndStatusOrderByGeneratedAtAsc(payment.getSubscriptionNumber(),
                        "PENDING");
        Bill latestMonthlyBill = paymentService.getLatestMonthlyBill(payment.getSubscriptionNumber());

        // Total balance includes all pending bills (current month + any outstanding
        // from previous months)
        BigDecimal totalBalance = billRepository.getTotalPendingBalance(payment.getSubscriptionNumber());

        // Total amount charged for the current billing cycle (this month's usage only)
        BigDecimal currentBillAmount = latestMonthlyBill.getTotalAmount() != null ? latestMonthlyBill.getTotalAmount()
                : BigDecimal.ZERO;

        // Outstanding balance carried forward from previous billing cycles at the time
        // this bill was generated
        BigDecimal outstandingAtIssue = latestMonthlyBill.getOutstandingAtIssue() != null
                ? latestMonthlyBill.getOutstandingAtIssue()
                : BigDecimal.ZERO;

        // Total amount due for this billing cycle (current month charges + carried
        // forward outstanding balance)
        BigDecimal totalDue = currentBillAmount.add(outstandingAtIssue);

        BigDecimal amount = payment.getAmount();

        // Validate payment against system total
        validateAmount(amount, totalBalance);

        BigDecimal remaining = amount;

        // Allocate payment to oldest bills first
        for (Bill bill : bills) {

            if (remaining.compareTo(BigDecimal.ZERO) <= 0)
                break;

            BigDecimal due = bill.getBalanceDue() != null ? bill.getBalanceDue() : BigDecimal.ZERO;
            BigDecimal applied;

            if (remaining.compareTo(due) >= 0) {
                applied = due;
                remaining = remaining.subtract(due);
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setStatus("PAID");
            } else {
                applied = remaining;
                bill.setBalanceDue(due.subtract(remaining));
                remaining = BigDecimal.ZERO;
                bill.setStatus("PENDING");
            }

            billRepository.save(bill);

            // Record how payment was allocated to this bill
            paymentService.saveAllocation(payment.getPaymentId(), bill.getBillId(), applied);
        }

        // Determine if this payment fully settles the current billing cycle
        boolean isFull = amount.compareTo(totalDue) == 0;

        return isFull ? PaymentStatus.FULL : PaymentStatus.PARTIAL;

    }

    // Returns current payment status for frontend polling after PayHere redirect
    public String getPaymentStatus(String orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        return payment.getStatus().name();
    }

    public CurrentBillResponse getCurrentBillForCustomer() {
        String subscriptionNumber = "SK-2341"; // TODO: replace with JWT auth context

        return paymentService.getCurrentBill(subscriptionNumber);

    }

    public OutstandingBillsSummaryResponse getOutstandingBillsForCustomer() {
        String subscriptionNumber = "SK-2341"; // TODO: replace with JWT auth context

        return paymentService.getOutstandingBills(subscriptionNumber);
    }

    public PaginationResponse<PaymentHistoryItemResponse> getPaymentHistoryForCustomer(int page, int size, Integer year, PaymentMethod paymentMethod) {
        String subscriptionNumber = "SK-2341"; // TODO: replace with JWT auth context

        return paymentService.getPaymentHistory(subscriptionNumber, page, size, year, paymentMethod);
    }

}
