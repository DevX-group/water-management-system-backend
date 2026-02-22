package com.backend.water_management_system.service;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import com.backend.water_management_system.entity.Payment;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.dto.AddPaymentRequest;
import com.backend.water_management_system.dto.AddPaymentResponse;
import com.backend.water_management_system.dto.CustomerPaymentSummaryResponse;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.PaymentStatus;
import com.backend.water_management_system.entity.PaymentType;
import com.backend.water_management_system.exception.InvalidPaymentException;
import com.backend.water_management_system.repository.BillRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.PaymentRepository;

@Service
public class PaymentService {
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;

    public PaymentService(CustomerRepository customerRepository, PaymentRepository paymentRepository,
            BillRepository billRepository) {
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
    }

    public AddPaymentResponse addPayment(AddPaymentRequest request) {
        if (request.getSubscriptionNumber() == null || request.getSubscriptionNumber().isBlank()) {
            throw new InvalidPaymentException("Subscription number is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }
        if (request.getStatus() == null) {
            throw new InvalidPaymentException("Payment status is required");
        }
        if (request.getPaymentType() == null) {
            throw new InvalidPaymentException("Payment type is required");
        }

        String subscriptionNumber = request.getSubscriptionNumber();
        BigDecimal amount = request.getAmount();
        BigDecimal oldValue = null;
        BigDecimal newValue = null;

        Customer customer = customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found with subscription number: " + subscriptionNumber));

        if (request.getPaymentType() == PaymentType.MONTHLY) {
            List<Bill> bills = billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber);
            Bill targetBill = null;

            for (Bill bill : bills) {
                BigDecimal due = bill.getBalanceDue();

                boolean hasDue = (due != null && due.compareTo(BigDecimal.ZERO) > 0);
                boolean notPaid = (bill.getStatus() == null || !bill.getStatus().equalsIgnoreCase("PAID"));

                if (hasDue && notPaid) {
                    targetBill = bill;
                    break;
                }
            }

            if (targetBill == null) {
                throw new InvalidPaymentException("No unpaid monthly bill found for this customer");
            }

            BigDecimal oldDue = targetBill.getBalanceDue();
            if (oldDue == null)
                oldDue = BigDecimal.ZERO;
            oldValue = oldDue;

            if (amount.compareTo(oldDue) > 0) {
                throw new InvalidPaymentException("Amount cannot be greater than monthly due");
            }

            if (request.getStatus() == PaymentStatus.FULL && amount.compareTo(oldDue) != 0) {
                throw new InvalidPaymentException("FULL payment must equal the monthly due amount");
            }

            if (request.getStatus() == PaymentStatus.PARTIAL && amount.compareTo(oldDue) == 0) {
                throw new InvalidPaymentException("PARTIAL payment must be less than the monthly due amount");
            }

            BigDecimal newDue = oldDue.subtract(amount);
            newValue = newDue;
            targetBill.setBalanceDue(newDue);

            if (newDue.compareTo(BigDecimal.ZERO) == 0) {
                targetBill.setStatus("PAID");
            } else if (targetBill.getStatus() == null || targetBill.getStatus().equalsIgnoreCase("PAID")) {
                targetBill.setStatus("PENDING");
            }

            billRepository.save(targetBill);
        } else if (request.getPaymentType() == PaymentType.OUTSTANDING) {
            BigDecimal oldBalance = customer.getOutstandingBalance();
            if (oldBalance == null)
                oldBalance = BigDecimal.ZERO;
            oldValue = oldBalance;

            if (amount.compareTo(oldBalance) > 0) {
                throw new InvalidPaymentException("Amount cannot be greater than outstanding balance");
            }
            if (request.getStatus() == PaymentStatus.FULL && amount.compareTo(oldBalance) != 0) {
                throw new InvalidPaymentException("FULL payment must equal outstanding balance");
            }
            if (request.getStatus() == PaymentStatus.PARTIAL && amount.compareTo(oldBalance) == 0) {
                throw new InvalidPaymentException("PARTIAL payment must be less than outstanding balance");
            }
            BigDecimal newBalance = oldBalance.subtract(amount);
            newValue = newBalance;
            customer.setOutstandingBalance(newBalance);
            customerRepository.save(customer);
        } else {
            throw new InvalidPaymentException("Unsupported payment type");
        }

        Payment payment = new Payment();

        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setSubscriptionNumber(subscriptionNumber);
        payment.setAmount(amount);
        payment.setStatus(request.getStatus());
        payment.setPaymentType(request.getPaymentType());
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        AddPaymentResponse response = new AddPaymentResponse("Payment added successfully");

        response.setSubscriptionNumber(subscriptionNumber);
        response.setOldBalance(oldValue);
        response.setNewBalance(newValue);
        response.setPaymentId(payment.getPaymentId());
        response.setStatus(request.getStatus());
        response.setPaymentType(request.getPaymentType());
        response.setCreatedAt(payment.getCreatedAt());

        return response;

    }

    public CustomerPaymentSummaryResponse getCustomerPaymentSummary(String subscriptionNumber) {

        if (subscriptionNumber == null || subscriptionNumber.isBlank()) {
            throw new InvalidPaymentException("Subscription number is required");
        }

        Customer customer = customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + subscriptionNumber));

        BigDecimal outstanding = customer.getOutstandingBalance();
        if (outstanding == null)
            outstanding = BigDecimal.ZERO;

        // Find latest unpaid bill (monthly due)
        List<Bill> bills = billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber);

        BigDecimal monthlyDue = BigDecimal.ZERO;
        String billStatus = "NO_BILL";

        for (Bill bill : bills) {
            BigDecimal due = bill.getBalanceDue();
            boolean hasDue = (due != null && due.compareTo(BigDecimal.ZERO) > 0);
            boolean notPaid = (bill.getStatus() == null || !bill.getStatus().equalsIgnoreCase("PAID"));

            if (hasDue && notPaid) {
                monthlyDue = due;
                billStatus = bill.getStatus() == null ? "PENDING" : bill.getStatus();
                break;
            }
        }

        BigDecimal totalDue = monthlyDue.add(outstanding);

        return new CustomerPaymentSummaryResponse(
                subscriptionNumber,
                monthlyDue,
                outstanding,
                totalDue,
                billStatus);
    }
}