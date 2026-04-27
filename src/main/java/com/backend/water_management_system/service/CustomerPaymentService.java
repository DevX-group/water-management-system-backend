package com.backend.water_management_system.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.config.PayHereConfig;
import com.backend.water_management_system.dto.AddPaymentRequest;
import com.backend.water_management_system.dto.CustomerPaymentResponse;
import com.backend.water_management_system.dto.PaymentResult;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Payment;
import com.backend.water_management_system.entity.PaymentStatus;
import com.backend.water_management_system.entity.PaymentType;
import com.backend.water_management_system.exception.InvalidPaymentException;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerPaymentService {

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PayHereConfig payHereConfig;
    private static final Logger log = LoggerFactory.getLogger(CustomerPaymentService.class);

    public CustomerPaymentResponse initiateCustomerPayment(AddPaymentRequest request) {

        paymentService.validateRequest(request);

        BigDecimal amount = request.getAmount();
        String subscriptionNumber = request.getSubscriptionNumber();

        if (request.getPaymentType() == PaymentType.MONTHLY) {
            Bill monthlyBill = paymentService.getLatestMonthlyBill(subscriptionNumber);
            paymentService.validateMonthlyPayment(amount, monthlyBill);

        } else if (request.getPaymentType() == PaymentType.OUTSTANDING) {
            List<Bill> outstandingBills = paymentService.getOutstandingBillsEntites(subscriptionNumber);
            paymentService.validateOutstandingPayment(amount, outstandingBills);

        }

        Payment payment = paymentService.createPaymentEntity(request);
        payment.setStatus(PaymentStatus.PENDING);

        String orderId = "PAY-" + payment.getPaymentId();
        String merchantId = payHereConfig.getMerchantId();
        String merchantSecret = payHereConfig.getMerchantSecret();
        String currency = "LKR";
        String amountFormatted = amount.setScale(2, RoundingMode.HALF_UP).toString();
        String hash = getMd5(merchantId + orderId + amountFormatted + currency + getMd5(merchantSecret));

        Customer customer = customerRepository.findBySubscriptionNumber(subscriptionNumber)
                .orElseThrow(() -> new InvalidPaymentException(
                        "Customer with subscription number " + subscriptionNumber + " not found"));

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

        payment.setOrderId(orderId);
        paymentRepository.save(payment);

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

    @Transactional
    public void handlePayhereNotification(java.util.Map<String, String> params) {

        log.info("params received = {}", params);

        Map<String, String> cleanParams = new HashMap<>();
        params.forEach((k, v) -> cleanParams.put(k.trim(), v.trim()));

        String orderId = cleanParams.get("order_id");
        String payherePaymentId = cleanParams.get("payment_id");
        String statusCode = cleanParams.get("status_code");
        String md5sig = cleanParams.get("md5sig");

        log.info("PayHere notify received. orderId={}, statusCode={}", orderId, statusCode);

        if (orderId == null || payherePaymentId == null || statusCode == null || md5sig == null) {
            log.warn("Missing required PayHere parameters. orderId={}", orderId);
            throw new InvalidPaymentException("Missing required parameters in PayHere notification");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                log.error("Payment not found for orderId={}", orderId);
                return new InvalidPaymentException("Payment with order ID " + orderId + " not found");
            });

        BigDecimal amount = new BigDecimal(cleanParams.get("amount"));

        if(amount.compareTo(payment.getAmount()) != 0) {
            throw new InvalidPaymentException("Amount mismatch in PayHere notification for order ID " + orderId);
        }

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

        log.info("Hash validation successful. orderId={}", orderId);

        if ("2".equals(statusCode)) { // SUCCESS
            // already processed → do nothing
            if (payment.getStatus() == PaymentStatus.FULL
                    || payment.getStatus() == PaymentStatus.PARTIAL) {

                log.info("Payment already processed. orderId={}, status={}", orderId, payment.getStatus());

                // Optionally update PayHere ID if missing
                if (payment.getPayherePaymentId() == null ) {
                    payment.setPayherePaymentId(payherePaymentId);
                    paymentRepository.save(payment);
                    log.info("Updated missing PayHere payment ID. orderId={}", orderId);
                }
                return;
            }

            log.info("Processing SUCCESS payment. orderId={}, type={}", orderId, payment.getPaymentType());

            // First-time success processing
            payment.setPayherePaymentId(payherePaymentId);

            PaymentResult result = processPayherePayment(payment, amount);
            payment.setStatus(result.getStatus());

            log.info("Payment processed successfully. orderId={}, finalStatus={}", orderId, result.getStatus());

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

    }

    public PaymentResult processPayherePayment(Payment payment, BigDecimal amount) {
        PaymentResult result;

        if (payment.getPaymentType() == PaymentType.MONTHLY) {
            Bill monthlyBill = paymentService.getLatestMonthlyBill(payment.getSubscriptionNumber());
            result = paymentService.processMonthlyPayment(payment, amount, monthlyBill);

        } else if (payment.getPaymentType() == PaymentType.OUTSTANDING) {
            List<Bill> outstandingBills = paymentService.getOutstandingBillsEntites(payment.getSubscriptionNumber());
            result = paymentService.processOutstandingPayment(payment, amount, outstandingBills);

        } else {
            throw new InvalidPaymentException("Unknown payment type for payment ID " + payment.getPaymentId());
        }
        return result;
    }
}
