package com.backend.water_management_system.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.dto.AddPaymentRequest;
import com.backend.water_management_system.dto.AddPaymentResponse;
import com.backend.water_management_system.dto.CurrentBillResponse;
import com.backend.water_management_system.dto.CustomerPaymentSummaryResponse;
import com.backend.water_management_system.dto.OutstandingBillResponse;
import com.backend.water_management_system.dto.OutstandingBillsSummaryResponse;
import com.backend.water_management_system.dto.PaymentHistoryItemResponse;
import com.backend.water_management_system.dto.PaymentResult;
import com.backend.water_management_system.dto.RecentPaymentResponse;
import com.backend.water_management_system.dto.PaymentCustomerInfoResponse;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Payment;
import com.backend.water_management_system.entity.PaymentAllocation;
import com.backend.water_management_system.entity.PaymentMethod;
import com.backend.water_management_system.entity.PaymentStatus;
import com.backend.water_management_system.entity.PaymentType;
import com.backend.water_management_system.exception.CustomerNotFoundException;
import com.backend.water_management_system.exception.InvalidPaymentException;
import com.backend.water_management_system.repository.BillRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.PaymentAllocationRepository;
import com.backend.water_management_system.repository.PaymentRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;

    @Transactional
    public AddPaymentResponse addPayment(AddPaymentRequest request) {

        validateRequest(request);

        String subscriptionNumber = request.getSubscriptionNumber();
        BigDecimal amount = request.getAmount();

        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found with subscription number: " + subscriptionNumber));

        Bill monthlyBill = null;
        List<Bill> outstandingBills = null;

        if (request.getPaymentType() == PaymentType.MONTHLY) {
            monthlyBill = getLatestMonthlyBill(subscriptionNumber);
            validateMonthlyPayment(amount, monthlyBill);
        } else if (request.getPaymentType() == PaymentType.OUTSTANDING) {
            outstandingBills = getOutstandingBillsEntites(subscriptionNumber);
            validateOutstandingPayment(amount, outstandingBills);
        }

        Payment payment = createPaymentEntity(request);

        PaymentResult result;

        if (request.getPaymentType() == PaymentType.MONTHLY) {
            result = processMonthlyPayment(payment, amount, monthlyBill);
        } else if (request.getPaymentType() == PaymentType.OUTSTANDING) {
            result = processOutstandingPayment(payment, amount, outstandingBills);
        } else {
            throw new InvalidPaymentException("Unsupported payment type");
        }

        payment.setStatus(result.getStatus());
        paymentRepository.save(payment);

        return buildResponse(payment, result, subscriptionNumber, request.getPaymentType(), request.getPaymentMethod());
    }

    public void validateRequest(AddPaymentRequest request) {
        if (request.getSubscriptionNumber() == null || request.getSubscriptionNumber().isBlank()) {
            throw new InvalidPaymentException("Subscription number is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }
        if (request.getPaymentType() == null) {
            throw new InvalidPaymentException("Payment type is required");
        }
        if (request.getPaymentMethod() == null) {
            throw new InvalidPaymentException("Payment method is required");
        }
    }

    public Payment createPaymentEntity(AddPaymentRequest request) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setSubscriptionNumber(request.getSubscriptionNumber());
        payment.setAmount(request.getAmount());
        payment.setPaymentType(request.getPaymentType());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }

    public Bill getLatestMonthlyBill(String subscriptionNumber) {
        return billRepository
                .findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                        subscriptionNumber, BigDecimal.ZERO)
                .orElseThrow(() -> new InvalidPaymentException("No unpaid monthly bill found"));
    }

    public List<Bill> getOutstandingBillsEntites(String subscriptionNumber) {

        Bill latest = billRepository
                .findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber)
                .orElseThrow(() -> new InvalidPaymentException("No bills found"));

        List<Bill> bills = billRepository
                .findByCustomer_SubscriptionNumberAndBalanceDueGreaterThanAndBillDateBeforeOrderByBillDateAsc(
                        subscriptionNumber,
                        BigDecimal.ZERO,
                        latest.getBillDate());

        if (bills.isEmpty()) {
            throw new InvalidPaymentException("No outstanding bills found");
        }

        return bills;
    }

    public void saveAllocation(String paymentId, Long billId, BigDecimal amount) {
        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setPaymentId(paymentId);
        allocation.setBillId(billId);
        allocation.setAmount(amount);
        paymentAllocationRepository.save(allocation);
    }

    public AddPaymentResponse buildResponse(Payment payment, PaymentResult result,
            String subscriptionNumber, PaymentType type, PaymentMethod method) {

        AddPaymentResponse response = new AddPaymentResponse();

        response.setMessage("Payment added successfully");
        response.setSubscriptionNumber(subscriptionNumber);
        response.setOldBalance(result.getOldBalance());
        response.setNewBalance(result.getNewBalance());
        response.setPaymentId(payment.getPaymentId());
        response.setStatus(result.getStatus());
        response.setPaymentType(type);
        response.setPaymentMethod(method);
        response.setCreatedAt(payment.getCreatedAt());

        return response;
    }

    public void validateMonthlyPayment(BigDecimal amount, Bill bill) {

        BigDecimal oldDue = bill.getBalanceDue() != null ? bill.getBalanceDue() : BigDecimal.ZERO;

        if (amount.compareTo(oldDue) > 0) {
            throw new InvalidPaymentException("Amount cannot be greater than monthly due");
        }
    }

    public PaymentResult processMonthlyPayment(Payment payment, BigDecimal amount, Bill bill) {

        BigDecimal oldDue = bill.getBalanceDue() != null ? bill.getBalanceDue() : BigDecimal.ZERO;

        BigDecimal total = bill.getTotalAmount() != null ? bill.getTotalAmount() : BigDecimal.ZERO;

        boolean isFull = oldDue.compareTo(total) == 0 && amount.compareTo(total) == 0;

        BigDecimal newDue = oldDue.subtract(amount);
        bill.setBalanceDue(newDue);

        bill.setStatus(newDue.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PENDING");

        billRepository.save(bill);

        saveAllocation(payment.getPaymentId(), bill.getBillId(), amount);

        return new PaymentResult(oldDue, newDue, isFull ? PaymentStatus.FULL : PaymentStatus.PARTIAL);
    }

    public void validateOutstandingPayment(BigDecimal amount, List<Bill> bills) {

        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (Bill bill : bills) {
            if (bill.getBalanceDue() != null) {
                totalOutstanding = totalOutstanding.add(bill.getBalanceDue());
            }
        }

        if (amount.compareTo(totalOutstanding) > 0) {
            throw new InvalidPaymentException("Amount exceeds total outstanding balance");
        }
    }

    public PaymentResult processOutstandingPayment(Payment payment, BigDecimal amount, List<Bill> bills) {

        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (Bill bill : bills) {
            if (bill.getBalanceDue() != null) {
                totalOutstanding = totalOutstanding.add(bill.getBalanceDue());
            }
        }

        if (amount.compareTo(totalOutstanding) > 0) {
            throw new InvalidPaymentException("Amount exceeds total outstanding balance");
        }

        BigDecimal remaining = amount;
        BigDecimal oldValue = totalOutstanding;

        for (Bill bill : bills) {

            if (remaining.compareTo(BigDecimal.ZERO) <= 0)
                break;

            BigDecimal due = bill.getBalanceDue();
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
            saveAllocation(payment.getPaymentId(), bill.getBillId(), applied);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidPaymentException("Amount exceeds total outstanding balance");
        }

        boolean isFull = amount.compareTo(totalOutstanding) == 0;

        return new PaymentResult(oldValue, BigDecimal.ZERO,
                isFull ? PaymentStatus.FULL : PaymentStatus.PARTIAL);
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

    public List<PaymentHistoryItemResponse> getPaymentHistory(String subscriptionNumber) {

        if (subscriptionNumber == null || subscriptionNumber.isBlank()) {
            throw new InvalidPaymentException("Subscription number is required");
        }

        if (!customerRepository.existsById(subscriptionNumber)) {
            throw new CustomerNotFoundException("Customer not found: " + subscriptionNumber);
        }

        List<PaymentStatus> validStatuses = List.of(PaymentStatus.FULL, PaymentStatus.PARTIAL);

        return paymentRepository
                .findBySubscriptionNumberAndStatusInOrderByCreatedAtDesc(subscriptionNumber, validStatuses)
                .stream()
                .map(p -> PaymentHistoryItemResponse.builder()
                        .paymentId(p.getPaymentId())
                        .subscriptionNumber(p.getSubscriptionNumber())
                        .amount(p.getAmount())
                        .status(p.getStatus())
                        .paymentType(p.getPaymentType())
                        .paymentMethod(p.getPaymentMethod())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();
    }

    public CurrentBillResponse getCurrentBill(String subscriptionNumber) {

        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + subscriptionNumber));

        Bill latest = billRepository.findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("No bills found for customer: " + subscriptionNumber));

        BigDecimal total = latest.getTotalAmount() == null ? BigDecimal.ZERO : latest.getTotalAmount();
        BigDecimal balance = latest.getBalanceDue() == null ? BigDecimal.ZERO : latest.getBalanceDue();
        BigDecimal alreadyPaid = total.subtract(balance);

        return new CurrentBillResponse(
                latest.getBillId(),
                latest.getBillingPeriod(),
                latest.getBillDate(),
                total,
                alreadyPaid,
                balance,
                latest.getStatus() == null ? "PENDING" : latest.getStatus());
    }

    public OutstandingBillsSummaryResponse getOutstandingBills(String subscriptionNumber) {

        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + subscriptionNumber));

        Bill latest = billRepository.findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber)
                .orElseThrow(() -> new InvalidPaymentException("No bills found"));

        List<Bill> bills = billRepository
                .findByCustomer_SubscriptionNumberAndBalanceDueGreaterThanAndBillDateBeforeOrderByBillDateAsc(
                        subscriptionNumber, BigDecimal.ZERO, latest.getBillDate());

        if (bills.isEmpty()) {
            return new OutstandingBillsSummaryResponse(List.of(), BigDecimal.ZERO);
        }

        List<OutstandingBillResponse> outstandingBills = bills.stream().map(b -> {

            BigDecimal total = b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount();
            BigDecimal balance = b.getBalanceDue() == null ? BigDecimal.ZERO : b.getBalanceDue();
            BigDecimal paid = total.subtract(balance);

            return new OutstandingBillResponse(
                    b.getBillId(),
                    b.getBillingPeriod(),
                    b.getBillDate(),
                    balance,
                    b.getStatus() == null ? "PENDING" : b.getStatus(),
                    total,
                    paid);

        }).toList();

        BigDecimal totalOutstandingAmount = outstandingBills.stream()
                .map(OutstandingBillResponse::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OutstandingBillsSummaryResponse(outstandingBills, totalOutstandingAmount);
    }

    public PaymentCustomerInfoResponse getPaymentCustomerInfo(String subscriptionNumber) {
        Customer customer = customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + subscriptionNumber));

        return new PaymentCustomerInfoResponse(
                customer.getSubscriptionNumber(),
                customer.getAccountHolderName(),
                customer.getRegion().getRegionName(),
                customer.getNic());

    }

    public List<RecentPaymentResponse> getRecentPayments(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtDesc(pageable);
        return payments.stream()
                .map(p -> {
                    RecentPaymentResponse res = new RecentPaymentResponse();
                    res.setPaymentId(p.getPaymentId());
                    res.setSubscriptionNumber(p.getSubscriptionNumber());
                    res.setAmountPaid(p.getAmount());
                    res.setStatus(p.getStatus().name());
                    res.setCreatedAt(p.getCreatedAt());

                    Customer customer = customerRepository.findBySubscriptionNumber(p.getSubscriptionNumber())
                            .orElse(null);

                    res.setAccountHolderName(
                            customer != null ? customer.getAccountHolderName() : "Unknown");

                    return res;
                })
                .toList();
    }

    @Transactional
    public AddPaymentResponse updatePayment(String paymentId, BigDecimal newAmount) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }

        String subscriptionNumber = payment.getSubscriptionNumber();

        BigDecimal oldBalance = BigDecimal.ZERO;
        BigDecimal newBalance = BigDecimal.ZERO;

        reversePaymentEffect(payment);

        payment.setAmount(newAmount);

        PaymentStatus newStatus;

        newStatus = applyPaymentEffect(payment);

        payment.setStatus(newStatus);

        paymentRepository.save(payment);

        AddPaymentResponse response = new AddPaymentResponse();
        response.setMessage("Payment updated successfully");
        response.setSubscriptionNumber(subscriptionNumber);
        response.setOldBalance(oldBalance);
        response.setNewBalance(newBalance);
        response.setPaymentId(payment.getPaymentId());
        response.setStatus(payment.getStatus());
        response.setPaymentType(payment.getPaymentType());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setCreatedAt(payment.getCreatedAt());

        return response;
    }

    public void reversePaymentEffect(Payment payment) {

        List<PaymentAllocation> allocations = paymentAllocationRepository.findByPaymentId(payment.getPaymentId());

        for (PaymentAllocation alloc : allocations) {

            Bill bill = billRepository.findById(alloc.getBillId())
                    .orElseThrow(() -> new RuntimeException("Bill not found"));

            BigDecimal currentDue = bill.getBalanceDue();

            bill.setBalanceDue(currentDue.add(alloc.getAmount()));
            bill.setStatus("PENDING");

            billRepository.save(bill);
        }

        paymentAllocationRepository.deleteAll(allocations);
    }

    public PaymentStatus applyPaymentEffect(Payment payment) {
        if (payment.getPaymentType() == PaymentType.MONTHLY) {
            return reapplyMonthlyPayment(payment);
        } else if (payment.getPaymentType() == PaymentType.OUTSTANDING) {
            return reapplyOutstandingPayment(payment);
        }
        throw new RuntimeException("Unsupported payment type");
    }

    public PaymentStatus reapplyMonthlyPayment(Payment payment) {
        List<Bill> bills = billRepository
                .findByCustomer_SubscriptionNumberOrderByBillDateDesc(
                        payment.getSubscriptionNumber());

        if (bills.isEmpty()) {
            throw new RuntimeException("No bills found");
        }

        Bill latest = bills.get(0);

        BigDecimal due = latest.getBalanceDue();
        if (due == null) {
            due = BigDecimal.ZERO;
        }
        BigDecimal amount = payment.getAmount();

        if (amount.compareTo(due) > 0) {
            throw new InvalidPaymentException("Payment amount exceeds current balance due");
        }

        BigDecimal appliedAmount = amount;

        latest.setBalanceDue(due.subtract(appliedAmount));
        BigDecimal total = latest.getTotalAmount();
        if (total == null)
            total = BigDecimal.ZERO;
        boolean isFullOneShot = due.compareTo(total) == 0 && amount.compareTo(total) == 0;

        latest.setStatus(latest.getBalanceDue().compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PENDING");

        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setPaymentId(payment.getPaymentId());
        allocation.setBillId(latest.getBillId());
        allocation.setAmount(appliedAmount);

        paymentAllocationRepository.save(allocation);

        billRepository.save(latest);

        return isFullOneShot ? PaymentStatus.FULL : PaymentStatus.PARTIAL;

    }

    public PaymentStatus reapplyOutstandingPayment(Payment payment) {
        BigDecimal remaining = payment.getAmount();

        List<Bill> bills = billRepository
                .findByCustomer_SubscriptionNumberOrderByBillDateAsc(
                        payment.getSubscriptionNumber());

        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (Bill bill : bills) {
            BigDecimal due = bill.getBalanceDue();
            if (due != null && due.compareTo(BigDecimal.ZERO) > 0) {
                totalOutstanding = totalOutstanding.add(due);
            }
        }

        boolean isFullOneShot = payment.getAmount().compareTo(totalOutstanding) == 0;

        if (payment.getAmount().compareTo(totalOutstanding) > 0) {
            throw new InvalidPaymentException("Payment amount exceeds total outstanding balance");
        }

        for (Bill bill : bills) {

            if (remaining.compareTo(BigDecimal.ZERO) <= 0)
                break;

            BigDecimal due = bill.getBalanceDue();

            if (due == null || due.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal appliedAmount;

            if (remaining.compareTo(due) >= 0) {
                // fully pay this bill
                appliedAmount = due;
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setStatus("PAID");
            } else {
                // partial payment
                appliedAmount = remaining;
                bill.setBalanceDue(due.subtract(remaining));
                bill.setStatus("PENDING");
            }

            PaymentAllocation allocation = new PaymentAllocation();
            allocation.setPaymentId(payment.getPaymentId());
            allocation.setBillId(bill.getBillId());
            allocation.setAmount(appliedAmount);

            paymentAllocationRepository.save(allocation);

            remaining = remaining.subtract(appliedAmount);

            billRepository.save(bill);
        }

        return isFullOneShot ? PaymentStatus.FULL : PaymentStatus.PARTIAL;
    }

    @Transactional
    public void deletePayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        reversePaymentEffect(payment);
        paymentRepository.delete(payment);
    }
}