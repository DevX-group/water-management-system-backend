package com.backend.water_management_system.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.config.PayHereConfig;
import com.backend.water_management_system.dto.AddPaymentRequest;
import com.backend.water_management_system.dto.CustomerPaymentResponse;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Payment;
import com.backend.water_management_system.entity.PaymentStatus;
import com.backend.water_management_system.entity.PaymentType;
import com.backend.water_management_system.exception.InvalidPaymentException;
import com.backend.water_management_system.repository.BillRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.PaymentAllocationRepository;
import com.backend.water_management_system.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerPaymentService {

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentService paymentService;
    private final PayHereConfig payHereConfig;

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
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext.toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
