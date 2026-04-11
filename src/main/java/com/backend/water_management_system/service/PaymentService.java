package com.backend.water_management_system.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.query.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.dto.AddPaymentRequest;
import com.backend.water_management_system.dto.AddPaymentResponse;
import com.backend.water_management_system.dto.CurrentBillResponse;
import com.backend.water_management_system.dto.CustomerPaymentSummaryResponse;
import com.backend.water_management_system.dto.OutstandingBillItemResponse;
import com.backend.water_management_system.dto.PaymentHistoryItemResponse;
import com.backend.water_management_system.dto.RecentPaymentResponse;
import com.backend.water_management_system.dto.PaymentCustomerInfoResponse;
import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Payment;
import com.backend.water_management_system.entity.PaymentAllocation;
import com.backend.water_management_system.entity.PaymentStatus;
import com.backend.water_management_system.entity.PaymentType;
import com.backend.water_management_system.exception.InvalidPaymentException;
import com.backend.water_management_system.repository.BillRepository;
import com.backend.water_management_system.repository.CustomerRepository;
import com.backend.water_management_system.repository.PaymentAllocationRepository;
import com.backend.water_management_system.repository.PaymentRepository;

import jakarta.transaction.Transactional;

@Service
public class PaymentService {

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;

    public PaymentService(CustomerRepository customerRepository,
            PaymentRepository paymentRepository,
            BillRepository billRepository,
            PaymentAllocationRepository paymentAllocationRepository) {
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
    }

    @Transactional
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

        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found with subscription number: " + subscriptionNumber));

        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setSubscriptionNumber(subscriptionNumber);
        payment.setAmount(amount);
        payment.setPaymentType(request.getPaymentType());
        payment.setCreatedAt(LocalDateTime.now());

        if (request.getPaymentType() == PaymentType.MONTHLY) {

            Bill targetBill = billRepository
                    .findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                            subscriptionNumber,
                            BigDecimal.ZERO)
                    .orElseThrow(() -> new InvalidPaymentException("No unpaid monthly bill found"));

            BigDecimal oldDue = targetBill.getBalanceDue();
            if (oldDue == null)
                oldDue = BigDecimal.ZERO;
            oldValue = oldDue;

            if (amount.compareTo(oldDue) > 0) {
                throw new InvalidPaymentException("Amount cannot be greater than monthly due");
            }

            BigDecimal total = targetBill.getTotalAmount();
            if (total == null)
                total = BigDecimal.ZERO;

            boolean isFullOneShot = oldDue.compareTo(total) == 0 && amount.compareTo(total) == 0;

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

            PaymentAllocation allocation = new PaymentAllocation();
            allocation.setPaymentId(payment.getPaymentId());
            allocation.setBillId(targetBill.getBillId());
            allocation.setAmount(amount);

            paymentAllocationRepository.save(allocation);

        } else if (request.getPaymentType() == PaymentType.OUTSTANDING) {

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

            BigDecimal totalOutstanding = BigDecimal.ZERO;

            for (Bill bill : bills) {
                BigDecimal due = bill.getBalanceDue();
                if (due != null && due.compareTo(BigDecimal.ZERO) > 0) {
                    totalOutstanding = totalOutstanding.add(due);
                }
            }

            BigDecimal remainingAmount = amount;
            oldValue = BigDecimal.ZERO;

            for (Bill bill : bills) {

                BigDecimal due = bill.getBalanceDue();

                oldValue = oldValue.add(due);

                if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal appliedAmount;

                if (remainingAmount.compareTo(due) >= 0) {
                    // Full pay this bill
                    appliedAmount = due;
                    remainingAmount = remainingAmount.subtract(due);
                    bill.setBalanceDue(BigDecimal.ZERO);
                    bill.setStatus("PAID");
                } else {
                    // Partial pay this bill
                    appliedAmount = remainingAmount;
                    bill.setBalanceDue(due.subtract(remainingAmount));
                    remainingAmount = BigDecimal.ZERO;
                    bill.setStatus("PENDING");
                }

                billRepository.save(bill);

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setPaymentId(payment.getPaymentId());
                allocation.setBillId(bill.getBillId());
                allocation.setAmount(appliedAmount);

                paymentAllocationRepository.save(allocation);
            }

            if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                throw new InvalidPaymentException("Amount exceeds total outstanding balance");
            }

            newValue = BigDecimal.ZERO; // optional for response

            boolean isFullOneShot = amount.compareTo(totalOutstanding) == 0;

            recordedStatus = isFullOneShot
                    ? PaymentStatus.FULL
                    : PaymentStatus.PARTIAL;
        }

        else {
            throw new InvalidPaymentException("Unsupported payment type");
        }

        payment.setStatus(recordedStatus);
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

        Bill latest = billRepository.findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("No bills found for customer: " + subscriptionNumber));

        return new CurrentBillResponse(
                latest.getBillId(),
                latest.getBillingPeriod(),
                latest.getBillDate(),
                latest.getTotalAmount() == null ? BigDecimal.ZERO : latest.getTotalAmount(),
                latest.getBalanceDue() == null ? BigDecimal.ZERO : latest.getBalanceDue(),
                latest.getStatus() == null ? "PENDING" : latest.getStatus());
    }

    public List<OutstandingBillItemResponse> getOutstandingBills(String subscriptionNumber) {
        
        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + subscriptionNumber));
        
        Bill latest = billRepository.findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber)
                .orElseThrow(() -> new InvalidPaymentException("No bills found"));
        
        List<Bill> bills = billRepository
                .findByCustomer_SubscriptionNumberAndBalanceDueGreaterThanAndBillDateBeforeOrderByBillDateAsc(
                        subscriptionNumber, BigDecimal.ZERO, latest.getBillDate());

        if (bills.isEmpty()) {
            return List.of();
        }

        return bills.stream().map(b -> {

            BigDecimal total = b.getTotalAmount();
            BigDecimal balance = b.getBalanceDue();
            BigDecimal paid = (total != null && balance != null) ? total.subtract(balance) : BigDecimal.ZERO;

            return new OutstandingBillItemResponse(
                b.getBillId(), 
                b.getBillingPeriod(), 
                b.getBillDate(), 
                balance,
                b.getStatus() == null ? "PENDING" : b.getStatus(), 
                total, 
                paid );

        }).toList();
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

        AddPaymentResponse response = new AddPaymentResponse("Payment updated successfully");
        response.setSubscriptionNumber(subscriptionNumber);
        response.setOldBalance(oldBalance);
        response.setNewBalance(newBalance);
        response.setPaymentId(payment.getPaymentId());
        response.setStatus(payment.getStatus());
        response.setPaymentType(payment.getPaymentType());
        response.setCreatedAt(payment.getCreatedAt());

        return response;
    }

    private void reversePaymentEffect(Payment payment) {

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

    private PaymentStatus reapplyMonthlyPayment(Payment payment){
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

    private PaymentStatus reapplyOutstandingPayment(Payment payment){
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