package com.backend.water_management_system.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.dto.AddPaymentRequest;
import com.backend.water_management_system.dto.AddPaymentResponse;
import com.backend.water_management_system.dto.CurrentBillResponse;
import com.backend.water_management_system.dto.CustomerPaymentSummaryResponse;
import com.backend.water_management_system.dto.OutstandingBillItemResponse;
import com.backend.water_management_system.dto.PaymentHistoryItemResponse;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Payment;
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

    public PaymentService(CustomerRepository customerRepository,
                          PaymentRepository paymentRepository,
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
        if (request.getPaymentType() == null) {
            throw new InvalidPaymentException("Payment type is required");
        }

        
        String subscriptionNumber = request.getSubscriptionNumber();
        BigDecimal amount = request.getAmount();

        BigDecimal oldValue = null;
        BigDecimal newValue = null;

        PaymentStatus recordedStatus = PaymentStatus.PARTIAL; // default

        Customer customer = customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found with subscription number: " + subscriptionNumber));

        if (request.getPaymentType() == PaymentType.MONTHLY) {

            List<Bill> bills = billRepository
                    .findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber);

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
            if (oldDue == null) oldDue = BigDecimal.ZERO;
            oldValue = oldDue;

            if (amount.compareTo(oldDue) > 0) {
                throw new InvalidPaymentException("Amount cannot be greater than monthly due");
            }

            BigDecimal total = targetBill.getTotalAmount();
            if (total == null) total = BigDecimal.ZERO;

            boolean isFullOneShot =
                    oldDue.compareTo(total) == 0 && amount.compareTo(total) == 0;

            recordedStatus = isFullOneShot ? PaymentStatus.FULL : PaymentStatus.PARTIAL;

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
            if (oldBalance == null) oldBalance = BigDecimal.ZERO;
            oldValue = oldBalance;

            if (amount.compareTo(oldBalance) > 0) {
                throw new InvalidPaymentException("Amount cannot be greater than outstanding balance");
            }

            
            recordedStatus = PaymentStatus.PARTIAL;

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
        payment.setStatus(recordedStatus);                  
        payment.setPaymentType(request.getPaymentType());
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        AddPaymentResponse response = new AddPaymentResponse("Payment added successfully");
        response.setSubscriptionNumber(subscriptionNumber);
        response.setOldBalance(oldValue);
        response.setNewBalance(newValue);
        response.setPaymentId(payment.getPaymentId());
        response.setStatus(recordedStatus);                 
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
        if (outstanding == null) outstanding = BigDecimal.ZERO;

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
                billStatus
        );
    }

    public List<PaymentHistoryItemResponse> getPaymentHistory(String subscriptionNumber) {

        if (subscriptionNumber == null || subscriptionNumber.isBlank()) {
            throw new InvalidPaymentException("Subscription number is required");
        }

        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + subscriptionNumber));

        return paymentRepository
                .findBySubscriptionNumberOrderByCreatedAtDesc(subscriptionNumber)
                .stream()
                .map(p -> new PaymentHistoryItemResponse(
                        p.getPaymentId(),
                        p.getSubscriptionNumber(),
                        p.getAmount(),
                        p.getStatus().name(),
                        p.getPaymentType().name(),
                        p.getCreatedAt()))
                .toList();
    }

    public CurrentBillResponse getCurrentBill(String subscriptionNumber) {

        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + subscriptionNumber));

        List<Bill> bills = billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber);

        if (bills.isEmpty()) {
            return null; 
        }

        Bill latest = bills.get(0);

        return new CurrentBillResponse(
                latest.getBillId(),
                latest.getBillingPeriod(),
                latest.getBillDate(),
                latest.getTotalAmount() == null ? BigDecimal.ZERO : latest.getTotalAmount(),
                latest.getBalanceDue() == null ? BigDecimal.ZERO : latest.getBalanceDue(),
                latest.getStatus() == null ? "PENDING" : latest.getStatus()
        );
    }

    public List<OutstandingBillItemResponse> getOutstandingBills(String subscriptionNumber) {

        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + subscriptionNumber));

        List<Bill> bills = billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber);

        if (bills.isEmpty()) {
            return List.of();
        }

        Bill latest = bills.get(0);

        return bills.stream()
                .filter(b -> !b.getBillId().equals(latest.getBillId())) // exclude current bill
                .filter(b -> b.getBalanceDue() != null && b.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .filter(b -> b.getStatus() == null || !b.getStatus().equalsIgnoreCase("PAID"))
                .map(b -> new OutstandingBillItemResponse(
                        b.getBillId(),
                        b.getBillingPeriod(),
                        b.getBillDate(),
                        b.getBalanceDue(),
                        b.getStatus() == null ? "PENDING" : b.getStatus()
                ))
                .toList();
    }
}