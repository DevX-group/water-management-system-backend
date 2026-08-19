package com.backend.water_management_system.payments.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.water_management_system.billing.dto.CurrentBillResponse;
import com.backend.water_management_system.billing.dto.OutstandingBillResponse;
import com.backend.water_management_system.billing.dto.OutstandingBillsSummaryResponse;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.common.dto.PaginationResponse;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.exceptions.CustomerNotFoundException;
import com.backend.water_management_system.payments.exceptions.InvalidPaymentException;
import com.backend.water_management_system.messaging.service.TriggeredMessageDispatcher;
import com.backend.water_management_system.notification.dto.NotificationRequest;
import com.backend.water_management_system.notification.enums.NotificationType;
import com.backend.water_management_system.notification.service.NotificationService;
import com.backend.water_management_system.messaging.enums.TriggerType;
import com.backend.water_management_system.payments.dto.AddPaymentRequest;
import com.backend.water_management_system.payments.dto.AddPaymentResponse;
import com.backend.water_management_system.payments.dto.CustomerPaymentSummaryResponse;
import com.backend.water_management_system.payments.dto.PaymentCustomerInfoResponse;
import com.backend.water_management_system.payments.dto.PaymentHistoryItemResponse;
import com.backend.water_management_system.payments.dto.PaymentResult;
import com.backend.water_management_system.payments.dto.RecentPaymentResponse;
import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.entity.PaymentAllocation;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentStatus;
import com.backend.water_management_system.payments.enums.PaymentType;
import com.backend.water_management_system.payments.repository.PaymentAllocationRepository;
import com.backend.water_management_system.payments.repository.PaymentRepository;
import com.backend.water_management_system.customer.repository.CustomerRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final TriggeredMessageDispatcher triggeredMessageDispatcher;
    private final NotificationService notificationService;

    // Main entry point for adding a manual payment. Handles validation, bill
    // selection and processing
    @Transactional
    public AddPaymentResponse addPayment(AddPaymentRequest request) {

        validateRequest(request);

        String subscriptionNumber = request.getSubscriptionNumber();
        BigDecimal amount = request.getAmount();

        // Ensure customer exists
        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found with subscription number: " + subscriptionNumber));

        Bill monthlyBill = null;
        List<Bill> outstandingBills = null;

        // Validate payment based on type
        if (request.getPaymentType() == PaymentType.MONTHLY) {
            monthlyBill = getLatestMonthlyBill(subscriptionNumber);
            validateMonthlyPayment(amount, monthlyBill);
        } else if (request.getPaymentType() == PaymentType.OUTSTANDING) {
            outstandingBills = getOutstandingBillsEntites(subscriptionNumber);
            validateOutstandingPayment(amount, outstandingBills);
        }

        Payment payment = createPaymentEntity(request);

        PaymentResult result;

        // Process payment based on type
        if (request.getPaymentType() == PaymentType.MONTHLY) {
            result = processMonthlyPayment(payment, amount, monthlyBill);
        } else if (request.getPaymentType() == PaymentType.OUTSTANDING) {
            result = processOutstandingPayment(payment, amount, outstandingBills);
        } else {
            throw new InvalidPaymentException("Unsupported payment type");
        }

        payment.setStatus(result.getStatus());
        paymentRepository.save(payment);

        if (request.getPaymentMethod() == PaymentMethod.MANUAL) {
            try {
                triggeredMessageDispatcher.dispatchTriggeredMessage(TriggerType.PAYMENT_CONFIRMED, payment);
            } catch (Exception ex) {
                log.warn("Failed to dispatch payment confirmation for {}: {}", payment.getPaymentId(), ex.getMessage());
            }
        }

        try {
            notificationService.sendNotification(
                    NotificationRequest.builder()
                            .subscriptionNumber(subscriptionNumber)
                            .notificationType(NotificationType.MANUAL_PAYMENT)
                            .title("Payment Successful")
                            .message("Your manual payment of Rs. " + amount + " has been added successfully.")
                            .build());
        } catch (Exception ex) {
            log.warn(
                    "Failed to send notification for payment {}: {}",
                    payment.getPaymentId(),
                    ex.getMessage());
        }

        return buildResponse(payment, result, subscriptionNumber, request.getPaymentType(), request.getPaymentMethod());
    }

    // Validates basic payment request fields.
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

    // Creates Payment entity from request
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

    // Fetch latest unpaid monthly bill.
    public Bill getLatestMonthlyBill(String subscriptionNumber) {
        return billRepository
                .findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                        subscriptionNumber, BigDecimal.ZERO)
                .orElseThrow(() -> new InvalidPaymentException("No unpaid monthly bill found"));
    }

    // Fetch all outstanding bills before latest bill date.
    public List<Bill> getOutstandingBillsEntites(String subscriptionNumber) {

        List<Bill> bills = billRepository.findOutstandingBillsExcludingLatest(subscriptionNumber);

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

    // Validate monthly payment amount
    public void validateMonthlyPayment(BigDecimal amount, Bill bill) {

        BigDecimal oldDue = bill.getBalanceDue() != null ? bill.getBalanceDue() : BigDecimal.ZERO;

        if (amount.compareTo(oldDue) > 0) {
            throw new InvalidPaymentException("Amount cannot be greater than monthly due");
        }
    }

    // Applies payment to monthly bill.
    public PaymentResult processMonthlyPayment(Payment payment, BigDecimal amount, Bill bill) {

        BigDecimal oldDue = bill.getBalanceDue() != null ? bill.getBalanceDue() : BigDecimal.ZERO;

        BigDecimal total = bill.getTotalAmount() != null ? bill.getTotalAmount() : BigDecimal.ZERO;

        // Check if this is a full payment in one shot
        boolean isFull = oldDue.compareTo(total) == 0 && amount.compareTo(total) == 0;

        BigDecimal newDue = oldDue.subtract(amount);
        bill.setBalanceDue(newDue);

        bill.setStatus(newDue.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PENDING");

        billRepository.save(bill);

        saveAllocation(payment.getPaymentId(), bill.getBillId(), amount);

        return new PaymentResult(oldDue, newDue, isFull ? PaymentStatus.FULL : PaymentStatus.PARTIAL);
    }

    // Validate outstanding payment amount
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

    // Applies payment across multiple outstanding bills.
    public PaymentResult processOutstandingPayment(Payment payment, BigDecimal amount, List<Bill> bills) {

        BigDecimal totalOutstandingBalance = BigDecimal.ZERO;

        // // Calculate total outstanding balance
        for (Bill bill : bills) {
            if (bill.getBalanceDue() != null) {
                totalOutstandingBalance = totalOutstandingBalance.add(bill.getBalanceDue());
            }
        }

        Bill monthlyBill = getLatestMonthlyBill(payment.getSubscriptionNumber());
        // Outstanding balance carried forward from previous billing cycles at the time
        // latest bill was generated
        BigDecimal totalOutstanding = monthlyBill != null && monthlyBill.getOutstandingAtIssue() != null
                ? monthlyBill.getOutstandingAtIssue()
                : BigDecimal.ZERO;

        BigDecimal remaining = amount;
        BigDecimal oldValue = totalOutstandingBalance;

        for (Bill bill : bills) {

            if (remaining.compareTo(BigDecimal.ZERO) <= 0)
                break;

            BigDecimal due = bill.getBalanceDue() != null
                    ? bill.getBalanceDue()
                    : BigDecimal.ZERO;
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

        // Check if this is a full payment in one shot
        boolean isFull = amount.compareTo(totalOutstanding) == 0;

        return new PaymentResult(oldValue, BigDecimal.ZERO,
                isFull ? PaymentStatus.FULL : PaymentStatus.PARTIAL);
    }

    public CustomerPaymentSummaryResponse getCustomerPaymentSummary(String subscriptionNumber) {

        if (subscriptionNumber == null || subscriptionNumber.isBlank()) {
            throw new InvalidPaymentException("Subscription number is required");
        }

        Customer customer = customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + subscriptionNumber));

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

    // Get payment history by subscription number
    public PaginationResponse<PaymentHistoryItemResponse> getPaymentHistory(String subscriptionNumber, int page,
            int size, Integer year, PaymentMethod paymentMethod) {

        if (subscriptionNumber == null || subscriptionNumber.isBlank()) {
            throw new InvalidPaymentException("Subscription number is required");
        }

        if (!customerRepository.existsById(subscriptionNumber)) {
            throw new CustomerNotFoundException(
                    "Customer not found: " + subscriptionNumber);
        }

        List<PaymentStatus> validStatuses = List.of(PaymentStatus.FULL, PaymentStatus.PARTIAL);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Payment> payments = paymentRepository
                .findBySubscriptionNumberAndFilters(subscriptionNumber, validStatuses, year, paymentMethod, pageable);

        List<PaymentHistoryItemResponse> content = payments.getContent()
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

        return PaginationResponse.<PaymentHistoryItemResponse>builder()
                .content(content)
                .currentPage(payments.getNumber())
                .totalPages(payments.getTotalPages())
                .totalElements(payments.getTotalElements())
                .pageSize(payments.getSize())
                .last(payments.isLast())
                .build();
    }

    // Get current billl details for frontend.
    public CurrentBillResponse getCurrentBill(String subscriptionNumber) {

        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + subscriptionNumber));

        Optional<Bill> latestOpt = billRepository
                .findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber);

        if (latestOpt.isEmpty()) {
            return null;
        }

        Bill latest = latestOpt.get();

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

    // Get outstanding bills details for frontend.
    public OutstandingBillsSummaryResponse getOutstandingBills(String subscriptionNumber) {

        customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + subscriptionNumber));

        List<Bill> bills = billRepository.findOutstandingBillsExcludingLatest(subscriptionNumber);

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

    // Get customer details.
    public PaymentCustomerInfoResponse getPaymentCustomerInfo(String subscriptionNumber) {
        Customer customer = customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + subscriptionNumber));

        return new PaymentCustomerInfoResponse(
                customer.getSubscriptionNumber(),
                customer.getAccountHolderName(),
                customer.getRegion().getRegionName(),
                customer.getConnectionType(),
                customer.getNic());

    }

    // Get recently added payments(manual and bank transfer) by admin.
    public List<RecentPaymentResponse> getRecentPayments(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Payment> payments = paymentRepository.findByPaymentMethodInOrderByCreatedAtDesc(
                List.of(PaymentMethod.MANUAL, PaymentMethod.BANK_TRANSFER), pageable);
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
                    res.setPaymentMethod(p.getPaymentMethod());
                    res.setPaymentType(p.getPaymentType());

                    return res;
                })
                .toList();
    }

    // Main method to handle paymet updation.
    @Transactional
    public AddPaymentResponse updatePayment(String paymentId, BigDecimal newAmount) {

        // Fetch existing payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        // Validate new amount
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }

        String subscriptionNumber = payment.getSubscriptionNumber();

        BigDecimal oldBalance = BigDecimal.ZERO;
        BigDecimal newBalance = BigDecimal.ZERO;

        // Step 1: Undo previous payment allocations from bills
        reversePaymentEffect(payment);

        // Step 2: Update payment amount
        payment.setAmount(newAmount);

        PaymentStatus newStatus;

        // Step 3: Re-apply payment logic based on type (MONTHLY / OUTSTANDING)
        newStatus = applyPaymentEffect(payment);

        payment.setStatus(newStatus);

        // Save updated payment.
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

    // Reverses all effects of a payment. (Restores bill balances and Removes
    // allocations)
    public void reversePaymentEffect(Payment payment) {

        // Fetch all allocations linked to this payment.
        List<PaymentAllocation> allocations = paymentAllocationRepository.findByPaymentId(payment.getPaymentId());

        for (PaymentAllocation alloc : allocations) {

            // Find affected bill
            Bill bill = billRepository.findById(alloc.getBillId())
                    .orElseThrow(() -> new RuntimeException("Bill not found"));

            // Restore previous due amount
            BigDecimal currentDue = bill.getBalanceDue();

            bill.setBalanceDue(currentDue.add(alloc.getAmount()));

            // Reset bill status since payment is reversed
            bill.setStatus("PENDING");

            billRepository.save(bill);
        }

        // Remove allocation records after reversal.
        paymentAllocationRepository.deleteAll(allocations);
    }

    // Routes re-application logic based on payment type.
    public PaymentStatus applyPaymentEffect(Payment payment) {
        if (payment.getPaymentType() == PaymentType.MONTHLY) {
            return reapplyMonthlyPayment(payment);
        } else if (payment.getPaymentType() == PaymentType.OUTSTANDING) {
            return reapplyOutstandingPayment(payment);
        }
        throw new InvalidPaymentException("Unsupported payment type");
    }

    // Re-applies a MONTHLY payment after reversal.
    public PaymentStatus reapplyMonthlyPayment(Payment payment) {

        Bill latest = getLatestMonthlyBill(payment.getSubscriptionNumber());

        BigDecimal due = latest.getBalanceDue();
        if (due == null) {
            due = BigDecimal.ZERO;
        }
        BigDecimal amount = payment.getAmount();

        if (amount.compareTo(due) > 0) {
            throw new InvalidPaymentException("Payment amount exceeds current balance due");
        }

        PaymentResult result = processMonthlyPayment(payment, amount, latest);

        return result.getStatus();

    }

    // Re-applies outstanding payment across multiple bills (FIFO order).
    public PaymentStatus reapplyOutstandingPayment(Payment payment) {
        BigDecimal amount = payment.getAmount();

        List<Bill> bills = getOutstandingBillsEntites(payment.getSubscriptionNumber());

        PaymentResult result = processOutstandingPayment(payment, amount, bills);

        return result.getStatus();
    }

    // Deletes a payment.
    @Transactional
    public void deletePayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        reversePaymentEffect(payment);
        paymentRepository.delete(payment);
    }
}