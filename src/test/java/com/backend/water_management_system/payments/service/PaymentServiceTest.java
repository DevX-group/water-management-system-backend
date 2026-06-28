package com.backend.water_management_system.payments.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.backend.water_management_system.billing.dto.CurrentBillResponse;
import com.backend.water_management_system.billing.dto.OutstandingBillResponse;
import com.backend.water_management_system.billing.dto.OutstandingBillsSummaryResponse;
import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.common.dto.PaginationResponse;
import com.backend.water_management_system.common.entity.Region;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.exceptions.CustomerNotFoundException;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.messaging.service.TriggeredMessageDispatcher;
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
import com.backend.water_management_system.payments.exceptions.InvalidPaymentException;
import com.backend.water_management_system.payments.repository.PaymentAllocationRepository;
import com.backend.water_management_system.payments.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

        // Mock all dependencies found in the PaymentService constructor
        @Mock
        private CustomerRepository customerRepository;

        @Mock
        private PaymentRepository paymentRepository;

        @Mock
        private BillRepository billRepository;

        @Mock
        private PaymentAllocationRepository paymentAllocationRepository;

        @Mock
        private TriggeredMessageDispatcher triggeredMessageDispatcher;

        // Inject mocks into the service we are testing
        @Spy
        @InjectMocks
        private PaymentService paymentService;

        @Test
        void testLatestMonthlyBill_Success() {
                String testSubscriptionNumber = "SUB123";

                // Create a dummy Bill
                Bill dummyBill = new Bill();
                dummyBill.setBillId(1L);
                dummyBill.setDueDate(LocalDate.now().plusMonths(1));
                dummyBill.setTotalAmount(new BigDecimal("100.00"));

                // Teach the mock repository how to behave
                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                testSubscriptionNumber,
                                BigDecimal.ZERO)).thenReturn(Optional.of(dummyBill));

                // Execute the method
                Bill result = paymentService.getLatestMonthlyBill(testSubscriptionNumber);

                // Verify the results
                assertNotNull(result, "The returned bill should not be null");
                assertEquals(1L, result.getBillId(), "The bill ID should match our dummy bill");
                assertEquals(new BigDecimal("100.00"), result.getTotalAmount(), "The total amount should match");
        }

        @Test
        void testLatestMonthlyBill_ThrowsExceptionWhenNotFound() {
                String testSubscriptionNumber = "SUB123";

                // Teach the mock to return an empty Optional
                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                testSubscriptionNumber,
                                BigDecimal.ZERO)).thenReturn(Optional.empty());

                // assertThrows checks if the lambda expression throws the specific exception
                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.getLatestMonthlyBill(testSubscriptionNumber));

                // Check if the error message is correct
                assertEquals("No unpaid monthly bill found", thrown.getMessage());
        }

        @Test
        void testOutstandingBills_Success() {
                String testSubscriptionNumber = "SUB123";

                // Create the "Outstanding Bill" dummy
                Bill oldOutstandingBill = new Bill();
                oldOutstandingBill.setBillId(1L);
                oldOutstandingBill.setBalanceDue(new BigDecimal("50.00"));

                // Put it in a list
                List<Bill> outstandingBillsList = List.of(oldOutstandingBill);

                // Teach the mock to return the list
                when(billRepository.findOutstandingBillsExcludingLatest(testSubscriptionNumber))
                                .thenReturn(outstandingBillsList);

                List<Bill> result = paymentService.getOutstandingBillsEntites(testSubscriptionNumber);

                assertNotNull(result, "The result list should not be null");
                assertEquals(1, result.size(), "There should be exactly 1 outstanding bill");
                assertEquals(1L, result.get(0).getBillId(), "The returned bill ID should match");
        }

        @Test
        void testOutstandingBills_ThrowsExceptionWhenNotFound() {
                String testSubscriptionNumber = "SUB123";

                when(billRepository.findOutstandingBillsExcludingLatest(testSubscriptionNumber))
                                .thenReturn(List.of()); // creates an empty list

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.getOutstandingBillsEntites(testSubscriptionNumber));

                assertEquals("No outstanding bills found", thrown.getMessage());
        }

        @Test
        void testValidateRequest_ThrowsExceptionIfSubscriptionNumberIsNull() {
                AddPaymentRequest invalidRequest = new AddPaymentRequest();
                invalidRequest.setSubscriptionNumber(null);
                invalidRequest.setAmount(new BigDecimal("100.00"));
                invalidRequest.setPaymentType(PaymentType.MONTHLY);
                invalidRequest.setPaymentMethod(PaymentMethod.MANUAL);

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.validateRequest(invalidRequest));
                assertEquals("Subscription number is required", thrown.getMessage());
        }

        @Test
        void testValidateRequest_ThrowsExceptionIfSubscriptionNumberIsBlank() {
                AddPaymentRequest invalidRequest = new AddPaymentRequest();
                invalidRequest.setSubscriptionNumber("   ");
                invalidRequest.setAmount(new BigDecimal("100.00"));
                invalidRequest.setPaymentType(PaymentType.MONTHLY);
                invalidRequest.setPaymentMethod(PaymentMethod.MANUAL);

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.validateRequest(invalidRequest));
                assertEquals("Subscription number is required", thrown.getMessage());
        }

        @Test
        void testValidateRequest_ThrowsExceptionIfAmountIsZero() {
                AddPaymentRequest invalidRequest = new AddPaymentRequest();
                invalidRequest.setSubscriptionNumber("SUB123");
                invalidRequest.setAmount(BigDecimal.ZERO);
                invalidRequest.setPaymentType(PaymentType.MONTHLY);
                invalidRequest.setPaymentMethod(PaymentMethod.MANUAL);

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.validateRequest(invalidRequest));
                assertEquals("Payment amount must be greater than zero", thrown.getMessage());
        }

        @Test
        void testValidateRequest_ThrowsExceptionIfAmountIsNegative() {
                AddPaymentRequest invalidRequest = new AddPaymentRequest();
                invalidRequest.setSubscriptionNumber("SUB123");
                invalidRequest.setAmount(new BigDecimal("-100.00"));
                invalidRequest.setPaymentType(PaymentType.MONTHLY);
                invalidRequest.setPaymentMethod(PaymentMethod.MANUAL);

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.validateRequest(invalidRequest));
                assertEquals("Payment amount must be greater than zero", thrown.getMessage());
        }

        @Test
        void testValidateRequest_ThrowsExceptionIfPaymentTypeIsNull() {
                AddPaymentRequest invalidRequest = new AddPaymentRequest();
                invalidRequest.setSubscriptionNumber("SUB123");
                invalidRequest.setAmount(new BigDecimal("100.00"));
                invalidRequest.setPaymentType(null);
                invalidRequest.setPaymentMethod(PaymentMethod.MANUAL);

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.validateRequest(invalidRequest));
                assertEquals("Payment type is required", thrown.getMessage());
        }

        @Test
        void testValidateRequest_ThrowsExceptionIfPaymentMethodIsNull() {
                AddPaymentRequest invalidRequest = new AddPaymentRequest();
                invalidRequest.setSubscriptionNumber("SUB123");
                invalidRequest.setAmount(new BigDecimal("100.00"));
                invalidRequest.setPaymentType(PaymentType.MONTHLY);
                invalidRequest.setPaymentMethod(null);

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.validateRequest(invalidRequest));
                assertEquals("Payment method is required", thrown.getMessage());
        }

        @Test
        void testValidateRequest_Success() {
                AddPaymentRequest validRequest = new AddPaymentRequest();
                validRequest.setSubscriptionNumber("SUB123");
                validRequest.setAmount(new BigDecimal("100.00"));
                validRequest.setPaymentType(PaymentType.MONTHLY);
                validRequest.setPaymentMethod(PaymentMethod.MANUAL);

                paymentService.validateRequest(validRequest);
        }

        @Test
        void testValidateMonthlyAmount_Success() {
                Bill bill = new Bill();
                bill.setBalanceDue(new BigDecimal("100"));

                paymentService.validateMonthlyPayment(new BigDecimal("50"), bill);

        }

        @Test
        void testValidateMonthlyAmount_ThrowsExceptionIfAmountIsGreaterThanBalanceDue() {
                Bill bill = new Bill();
                bill.setBalanceDue(new BigDecimal("100"));

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.validateMonthlyPayment(new BigDecimal("150"), bill));
                assertEquals("Amount cannot be greater than monthly due", thrown.getMessage());
        }

        @Test
        void testValidateOutstandingAmount_Success() {
                Bill bill1 = new Bill();
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);
                paymentService.validateOutstandingPayment(new BigDecimal("50"), bills);
        }

        @Test
        void testValidateOutstandingAmount_ThrowsExceptionIfAmountIsGreaterThanTotalOutstandingAmount() {
                Bill bill1 = new Bill();
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);
                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.validateOutstandingPayment(new BigDecimal("500"), bills));
                assertEquals("Amount exceeds total outstanding balance", thrown.getMessage());
        }

        @Test
        void testCreatePaymentEntity_Success() {
                AddPaymentRequest request = new AddPaymentRequest();
                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("200"));
                request.setPaymentType(PaymentType.MONTHLY);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                Payment payment = paymentService.createPaymentEntity(request);

                assertNotNull(payment.getPaymentId());
                assertEquals("SUB123", payment.getSubscriptionNumber());
                assertEquals(new BigDecimal("200"), payment.getAmount());
                assertNotNull(payment.getCreatedAt());
        }

        @Test
        void testProcessMonthlyPayment_FullPayment() {

                // Create payment
                Payment payment = new Payment();
                payment.setPaymentId("PAY123");

                // Create bill
                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setBalanceDue(new BigDecimal("100"));

                // Execute method
                PaymentResult result = paymentService.processMonthlyPayment(payment, new BigDecimal("100"), bill);

                // Verify bill updated correctly
                assertEquals(BigDecimal.ZERO, bill.getBalanceDue());

                assertEquals("PAID", bill.getStatus());

                // Verify returned payment result
                assertEquals(PaymentStatus.FULL, result.getStatus());

                assertEquals(new BigDecimal("100"), result.getOldBalance());

                assertEquals(BigDecimal.ZERO, result.getNewBalance());

                // Verify repository save called
                verify(billRepository).save(bill);

                // Verify allocation saved
                verify(paymentAllocationRepository).save(any());
        }

        @Test
        void testProcessMonthlyPayment_PartialPayment() {

                // Create payment
                Payment payment = new Payment();
                payment.setPaymentId("PAY123");

                // Create bill
                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setBalanceDue(new BigDecimal("100"));

                // Execute method
                PaymentResult result = paymentService.processMonthlyPayment(payment, new BigDecimal("50"), bill);

                // Verify bill updated correctly
                assertEquals(new BigDecimal("50"), bill.getBalanceDue());

                assertEquals("PENDING", bill.getStatus());

                // Verify returned payment result
                assertEquals(PaymentStatus.PARTIAL, result.getStatus());

                assertEquals(new BigDecimal("100"), result.getOldBalance());

                assertEquals(new BigDecimal("50"), result.getNewBalance());

                // Verify repository save called
                verify(billRepository).save(bill);

                // Verify allocation saved
                verify(paymentAllocationRepository).save(any());
        }

        @Test
        void testProcessMonthlyPayment_FullyClearsExistingPartialBill() {

                Payment payment = new Payment();
                payment.setPaymentId("PAY123");

                Bill bill = new Bill();
                bill.setBillId(1L);

                // Original total bill
                bill.setTotalAmount(new BigDecimal("100"));

                // Remaining balance only
                bill.setBalanceDue(new BigDecimal("50"));

                PaymentResult result = paymentService.processMonthlyPayment(payment, new BigDecimal("50"), bill);

                // Bill fully cleared
                assertEquals(BigDecimal.ZERO, bill.getBalanceDue());

                // Bill status should become PAID
                assertEquals("PAID", bill.getStatus());

                // Payment status should still be PARTIAL
                // because original due != total amount
                assertEquals(PaymentStatus.PARTIAL, result.getStatus());

                verify(billRepository).save(bill);

                verify(paymentAllocationRepository).save(any());
        }

        @Test
        void testProcessOutstandingPayment_PartialPaymentAcrossBills() {

                // Create payment
                Payment payment = new Payment();
                payment.setPaymentId("PAY123");
                payment.setSubscriptionNumber("SUB123");

                // Old outstanding bill
                Bill bill1 = new Bill();
                bill1.setBillId(1L);
                bill1.setBalanceDue(new BigDecimal("100"));

                // Another outstanding bill
                Bill bill2 = new Bill();
                bill2.setBillId(2L);
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);

                // Mock latest monthly bill
                Bill latestBill = new Bill();
                latestBill.setOutstandingAtIssue(new BigDecimal("300"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123",
                                BigDecimal.ZERO)).thenReturn(Optional.of(latestBill));

                // Execute method
                PaymentResult result = paymentService.processOutstandingPayment(payment, new BigDecimal("250"), bills);

                // Bill1 fully paid
                assertEquals(BigDecimal.ZERO, bill1.getBalanceDue());
                assertEquals("PAID", bill1.getStatus());

                // Bill2 partially paid
                assertEquals(new BigDecimal("50"), bill2.getBalanceDue());
                assertEquals("PENDING", bill2.getStatus());

                // Returned payment status
                assertEquals(PaymentStatus.PARTIAL, result.getStatus());

                // Verify saves happened
                verify(billRepository).save(bill1);
                verify(billRepository).save(bill2);

                // Two allocations should be saved
                verify(paymentAllocationRepository,
                                org.mockito.Mockito.times(2))
                                .save(any());
        }

        @Test
        void testProcessOutstandingPayment_FullPayment() {
                Payment payment = new Payment();
                payment.setPaymentId("PAY123");
                payment.setSubscriptionNumber("SUB123");

                Bill bill1 = new Bill();
                bill1.setBillId(1L);
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBillId(2L);
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);

                Bill latestBill = new Bill();
                latestBill.setOutstandingAtIssue(new BigDecimal("300"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123",
                                BigDecimal.ZERO)).thenReturn(Optional.of(latestBill));

                // Execute method
                PaymentResult result = paymentService.processOutstandingPayment(payment, new BigDecimal("300"), bills);

                // Bill1 fully paid
                assertEquals(BigDecimal.ZERO, bill1.getBalanceDue());
                assertEquals("PAID", bill1.getStatus());

                // Bill2 fully paid
                assertEquals(BigDecimal.ZERO, bill2.getBalanceDue());
                assertEquals("PAID", bill2.getStatus());

                // Payment status should be FULL
                // because outstandingAtIssue == total amount
                assertEquals(PaymentStatus.FULL, result.getStatus());

                // Verify saves happened
                verify(billRepository).save(bill1);
                verify(billRepository).save(bill2);

                // Two allocations should be saved
                verify(paymentAllocationRepository,
                                org.mockito.Mockito.times(2))
                                .save(any());
        }

        @Test
        void testProcessOutstandingPayment_FullyClearsExistingPartialBills() {
                Payment payment = new Payment();
                payment.setPaymentId("PAY123");
                payment.setSubscriptionNumber("SUB123");

                Bill bill1 = new Bill();
                bill1.setBillId(1L);
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBillId(2L);
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);

                Bill latestBill = new Bill();
                latestBill.setOutstandingAtIssue(new BigDecimal("400"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123",
                                BigDecimal.ZERO)).thenReturn(Optional.of(latestBill));

                PaymentResult result = paymentService.processOutstandingPayment(payment, new BigDecimal("300"), bills);

                // Bill1 fully paid
                assertEquals(BigDecimal.ZERO, bill1.getBalanceDue());
                assertEquals("PAID", bill1.getStatus());

                // Bill2 fully paid
                assertEquals(BigDecimal.ZERO, bill2.getBalanceDue());
                assertEquals("PAID", bill2.getStatus());

                // Payment status should still be PARTIAL
                // because outstandingAtIssue != total amount
                assertEquals(PaymentStatus.PARTIAL, result.getStatus());

                // Verify saves happened
                verify(billRepository).save(bill1);
                verify(billRepository).save(bill2);

                // Two allocations should be saved
                verify(paymentAllocationRepository,
                                org.mockito.Mockito.times(2))
                                .save(any());
        }

        @Test
        void testProcessOutstandingPayment_BreaksLoopWhenRemainingBecomesZero() {

                Payment payment = new Payment();
                payment.setPaymentId("PAY123");
                payment.setSubscriptionNumber("SUB123");

                Bill bill1 = new Bill();
                bill1.setBillId(1L);
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBillId(2L);
                bill2.setBalanceDue(new BigDecimal("200"));

                Bill bill3 = new Bill();
                bill3.setBillId(3L);
                bill3.setBalanceDue(new BigDecimal("300"));

                List<Bill> bills = List.of(bill1, bill2, bill3);

                Bill latestBill = new Bill();
                latestBill.setOutstandingAtIssue(new BigDecimal("600"));

                when(billRepository
                                .findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                                "SUB123",
                                                BigDecimal.ZERO))
                                .thenReturn(Optional.of(latestBill));

                // Amount exactly clears first two bills only
                PaymentResult result = paymentService.processOutstandingPayment(
                                payment,
                                new BigDecimal("300"),
                                bills);

                // First bill fully paid
                assertEquals(BigDecimal.ZERO, bill1.getBalanceDue());
                assertEquals("PAID", bill1.getStatus());

                // Second bill fully paid
                assertEquals(BigDecimal.ZERO, bill2.getBalanceDue());
                assertEquals("PAID", bill2.getStatus());

                // Third bill should remain untouched because loop breaks
                assertEquals(new BigDecimal("300"), bill3.getBalanceDue());

                // save() should NOT be called for third bill
                verify(billRepository,
                                org.mockito.Mockito.never())
                                .save(bill3);

                // Only two allocations saved
                verify(paymentAllocationRepository,
                                org.mockito.Mockito.times(2))
                                .save(any());

                assertEquals(PaymentStatus.PARTIAL, result.getStatus());
        }

        @Test
        void testAddPayment_MonthlyFullPaymentSuccess() {

                // Create request
                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("100"));
                request.setPaymentType(PaymentType.MONTHLY);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                // Mock customer
                Customer customer = new Customer();

                when(customerRepository.findById("SUB123")).thenReturn(Optional.of(customer));

                // Mock monthly bill
                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setBalanceDue(new BigDecimal("100"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123",
                                BigDecimal.ZERO))
                                .thenReturn(Optional.of(bill));

                // Execute method
                AddPaymentResponse response = paymentService.addPayment(request);

                // Verify response
                assertNotNull(response);

                assertEquals("Payment added successfully", response.getMessage());

                assertEquals("SUB123", response.getSubscriptionNumber());

                assertEquals(PaymentStatus.FULL, response.getStatus());

                // Verify payment saved
                verify(paymentRepository).save(any());

                // Verify bill updated
                verify(billRepository).save(bill);

                // Verify allocation saved
                verify(paymentAllocationRepository, org.mockito.Mockito.times(1)).save(any());

                // Verify dispatcher called
                verify(triggeredMessageDispatcher).dispatchPaymentConfirmed(any());
        }

        @Test
        void testAddPayment_MonthlyPartialPaymentSuccess() {

                // Create request
                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("50"));
                request.setPaymentType(PaymentType.MONTHLY);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                // Mock customer
                Customer customer = new Customer();

                when(customerRepository.findById("SUB123")).thenReturn(Optional.of(customer));

                // Mock monthly bill
                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setBalanceDue(new BigDecimal("100"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123",
                                BigDecimal.ZERO))
                                .thenReturn(Optional.of(bill));

                // Execute method
                AddPaymentResponse response = paymentService.addPayment(request);

                // Verify response
                assertNotNull(response);

                assertEquals("Payment added successfully", response.getMessage());

                assertEquals("SUB123", response.getSubscriptionNumber());

                assertEquals(PaymentStatus.PARTIAL, response.getStatus());

                // Verify payment saved
                verify(paymentRepository).save(any());

                // Verify bill updated
                verify(billRepository).save(bill);

                // Verify allocation saved
                verify(paymentAllocationRepository, org.mockito.Mockito.times(1)).save(any());

                // Verify dispatcher called
                verify(triggeredMessageDispatcher).dispatchPaymentConfirmed(any());
        }

        @Test
        void testAddPayment_MonthlyClearsExistingPartialBill() {

                // Create request
                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("50"));
                request.setPaymentType(PaymentType.MONTHLY);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                // Mock customer
                Customer customer = new Customer();

                when(customerRepository.findById("SUB123")).thenReturn(Optional.of(customer));

                // Bill already partially paid earlier
                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setBalanceDue(new BigDecimal("50"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123",
                                BigDecimal.ZERO))
                                .thenReturn(Optional.of(bill));

                // Execute method
                AddPaymentResponse response = paymentService.addPayment(request);

                // Bill fully cleared
                assertEquals(BigDecimal.ZERO, bill.getBalanceDue());

                assertEquals("PAID", bill.getStatus());

                // Payment status still PARTIAL
                assertEquals(PaymentStatus.PARTIAL, response.getStatus());

                verify(paymentRepository).save(any());

                verify(paymentAllocationRepository).save(any());

                verify(triggeredMessageDispatcher).dispatchPaymentConfirmed(any());
        }

        @Test
        void testAddPayment_OutstandingFullPaymentSuccess() {

                // Create request
                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("300"));
                request.setPaymentType(PaymentType.OUTSTANDING);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                // Mock customer
                Customer customer = new Customer();

                when(customerRepository.findById("SUB123")).thenReturn(Optional.of(customer));

                // Mock outstanding bills
                Bill bill1 = new Bill();
                bill1.setBillId(1L);
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBillId(2L);
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);

                when(billRepository.findOutstandingBillsExcludingLatest("SUB123"))
                                .thenReturn(bills);

                // Mock latest monthly bill
                // Required internally by processOutstandingPayment()
                Bill latestBill = new Bill();
                latestBill.setOutstandingAtIssue(new BigDecimal("300"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123",
                                BigDecimal.ZERO))
                                .thenReturn(Optional.of(latestBill));

                // Execute method
                AddPaymentResponse response = paymentService.addPayment(request);

                // Verify response
                assertNotNull(response);

                assertEquals("Payment added successfully", response.getMessage());

                assertEquals("SUB123", response.getSubscriptionNumber());

                assertEquals(PaymentStatus.FULL, response.getStatus());

                // Verify payment saved
                verify(paymentRepository).save(any());

                // Verify bills updated
                verify(billRepository).save(bill1);
                verify(billRepository).save(bill2);

                // Verify allocations saved
                verify(paymentAllocationRepository, org.mockito.Mockito.times(2)).save(any());

                // Verify dispatcher called
                verify(triggeredMessageDispatcher).dispatchPaymentConfirmed(any());
        }

        @Test
        void testAddPayment_OutstandingPartialPaymentSuccess() {

                // Create request
                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("250"));
                request.setPaymentType(PaymentType.OUTSTANDING);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                // Mock customer
                Customer customer = new Customer();

                when(customerRepository.findById("SUB123")).thenReturn(Optional.of(customer));

                // Mock outstanding bills
                Bill bill1 = new Bill();
                bill1.setBillId(1L);
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBillId(2L);
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);

                when(billRepository.findOutstandingBillsExcludingLatest("SUB123"))
                                .thenReturn(bills);

                // Mock latest monthly bill
                // Required internally by processOutstandingPayment()
                Bill latestBill = new Bill();
                latestBill.setOutstandingAtIssue(new BigDecimal("300"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123",
                                BigDecimal.ZERO))
                                .thenReturn(Optional.of(latestBill));

                // Execute method
                AddPaymentResponse response = paymentService.addPayment(request);

                // Verify response
                assertNotNull(response);

                assertEquals("Payment added successfully", response.getMessage());

                assertEquals("SUB123", response.getSubscriptionNumber());

                assertEquals(PaymentStatus.PARTIAL, response.getStatus());

                // Verify payment saved
                verify(paymentRepository).save(any());

                // Verify bills updated
                verify(billRepository).save(bill1);
                verify(billRepository).save(bill2);

                // Verify allocations saved
                verify(paymentAllocationRepository, org.mockito.Mockito.times(2)).save(any());

                // Verify dispatcher called
                verify(triggeredMessageDispatcher).dispatchPaymentConfirmed(any());
        }

        @Test
        void testAddPayment_OutstandingClearsExistingPartialBills() {

                // Create request
                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("300"));
                request.setPaymentType(PaymentType.OUTSTANDING);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                // Mock customer
                Customer customer = new Customer();

                when(customerRepository.findById("SUB123")).thenReturn(Optional.of(customer));

                // Mock outstanding bills
                Bill bill1 = new Bill();
                bill1.setBillId(1L);
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBillId(2L);
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);

                when(billRepository.findOutstandingBillsExcludingLatest("SUB123"))
                                .thenReturn(bills);

                // Mock latest monthly bill
                // Required internally by processOutstandingPayment()
                Bill latestBill = new Bill();
                latestBill.setOutstandingAtIssue(new BigDecimal("400"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123",
                                BigDecimal.ZERO))
                                .thenReturn(Optional.of(latestBill));

                // Execute method
                AddPaymentResponse response = paymentService.addPayment(request);

                // Verify response
                assertNotNull(response);

                assertEquals("Payment added successfully", response.getMessage());

                assertEquals("SUB123", response.getSubscriptionNumber());

                assertEquals(PaymentStatus.PARTIAL, response.getStatus());

                // Verify payment saved
                verify(paymentRepository).save(any());

                // Verify bills updated
                verify(billRepository).save(bill1);
                verify(billRepository).save(bill2);

                // Verify allocations saved
                verify(paymentAllocationRepository, org.mockito.Mockito.times(2)).save(any());

                // Verify dispatcher called
                verify(triggeredMessageDispatcher).dispatchPaymentConfirmed(any());
        }

        @Test
        void testAddPayment_ThrowsExceptionWhenCustomerNotFound() {

                // Create request
                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("100"));
                request.setPaymentType(PaymentType.MONTHLY);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                // Mock customer not found
                when(customerRepository.findById("SUB123")).thenReturn(Optional.empty());

                RuntimeException thrown = assertThrows(RuntimeException.class,
                                () -> paymentService.addPayment(request));

                assertEquals("Customer not found with subscription number: SUB123", thrown.getMessage());
        }

        @Test
        void testAddPayment_ThrowsExceptionWhenNoMonthlyBillFound() {

                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("100"));
                request.setPaymentType(PaymentType.MONTHLY);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                // Mock customer exists
                Customer customer = new Customer();

                when(customerRepository.findById("SUB123")).thenReturn(Optional.of(customer));

                // Mock no bill found
                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123", BigDecimal.ZERO))
                                .thenReturn(Optional.empty());

                InvalidPaymentException thrown = assertThrows(InvalidPaymentException.class,
                                () -> paymentService.addPayment(request));

                assertEquals("No unpaid monthly bill found", thrown.getMessage());
        }

        @Test
        void testAddPayment_ThrowsExceptionWhenNoOutstandingBillsFound() {

                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("100"));
                request.setPaymentType(PaymentType.OUTSTANDING);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                // Mock customer exists
                Customer customer = new Customer();

                when(customerRepository.findById("SUB123")).thenReturn(Optional.of(customer));

                // Mock no outstanding bills
                when(billRepository.findOutstandingBillsExcludingLatest("SUB123")).thenReturn(List.of());

                InvalidPaymentException thrown = assertThrows(InvalidPaymentException.class,
                                () -> paymentService.addPayment(request));

                assertEquals("No outstanding bills found", thrown.getMessage());
        }

        @Test
        void testAddPayment_DoesNotDispatchMessageForBankTransfer() {

                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("100"));
                request.setPaymentType(PaymentType.MONTHLY);
                request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

                Customer customer = new Customer();

                when(customerRepository.findById("SUB123")).thenReturn(Optional.of(customer));

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setBalanceDue(new BigDecimal("100"));

                when(billRepository.findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                "SUB123", BigDecimal.ZERO))
                                .thenReturn(Optional.of(bill));

                paymentService.addPayment(request);

                verify(triggeredMessageDispatcher, org.mockito.Mockito.never()).dispatchPaymentConfirmed(any());
        }

        @Test
        void testAddPayment_DispatchFailureDoesNotBreakPayment() {

                AddPaymentRequest request = new AddPaymentRequest();

                request.setSubscriptionNumber("SUB123");
                request.setAmount(new BigDecimal("100"));
                request.setPaymentType(PaymentType.MONTHLY);
                request.setPaymentMethod(PaymentMethod.MANUAL);

                Customer customer = new Customer();

                when(customerRepository.findById("SUB123"))
                                .thenReturn(Optional.of(customer));

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setBalanceDue(new BigDecimal("100"));

                when(billRepository
                                .findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                                                "SUB123", BigDecimal.ZERO))
                                .thenReturn(Optional.of(bill));

                doThrow(new RuntimeException("SMS service unavailable"))
                                .when(triggeredMessageDispatcher)
                                .dispatchPaymentConfirmed(any());
                AddPaymentResponse response = paymentService.addPayment(request);

                assertNotNull(response);

                assertEquals("Payment added successfully", response.getMessage());

                assertEquals(PaymentStatus.FULL, response.getStatus());

                // Verify payment still saved
                verify(paymentRepository).save(any());

                // Verify dispatcher attempted
                verify(triggeredMessageDispatcher).dispatchPaymentConfirmed(any());
        }

        @Test
        void testUpdatePayment_MonthlyFullToPartial() {

                Payment existingPayment = new Payment();
                existingPayment.setPaymentId("PAY123");
                existingPayment.setAmount(new BigDecimal("100"));
                existingPayment.setPaymentType(PaymentType.MONTHLY);
                existingPayment.setSubscriptionNumber("SUB123");

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setStatus("PAID");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);
                allocation.setAmount(new BigDecimal("100"));

                when(paymentRepository.findById("PAY123"))
                                .thenReturn(Optional.of(existingPayment));

                when(paymentAllocationRepository.findByPaymentId("PAY123"))
                                .thenReturn(List.of(allocation));

                when(billRepository.findById(1L))
                                .thenReturn(Optional.of(bill));

                doReturn(PaymentStatus.PARTIAL)
                                .when(paymentService)
                                .applyPaymentEffect(any(Payment.class));

                AddPaymentResponse response = paymentService.updatePayment("PAY123", new BigDecimal("50"));

                assertEquals(new BigDecimal("100"), bill.getBalanceDue());
                assertEquals("PENDING", bill.getStatus());
                assertEquals(PaymentStatus.PARTIAL, response.getStatus());

                verify(paymentRepository).save(existingPayment);
        }

        @Test
        void testUpdatePayment_MonthlyPartialToFull() {

                Payment existingPayment = new Payment();
                existingPayment.setPaymentId("PAY123");
                existingPayment.setAmount(new BigDecimal("50"));
                existingPayment.setPaymentType(PaymentType.MONTHLY);
                existingPayment.setSubscriptionNumber("SUB123");

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setStatus("PENDING");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);
                allocation.setAmount(new BigDecimal("50"));

                when(paymentRepository.findById("PAY123"))
                                .thenReturn(Optional.of(existingPayment));

                when(paymentAllocationRepository.findByPaymentId("PAY123"))
                                .thenReturn(List.of(allocation));

                when(billRepository.findById(1L))
                                .thenReturn(Optional.of(bill));

                doReturn(PaymentStatus.FULL)
                                .when(paymentService)
                                .applyPaymentEffect(any(Payment.class));

                AddPaymentResponse response = paymentService.updatePayment("PAY123", new BigDecimal("100"));

                assertEquals(new BigDecimal("50"), bill.getBalanceDue());
                assertEquals("PENDING", bill.getStatus());
                assertEquals(PaymentStatus.FULL, response.getStatus());

                verify(paymentRepository).save(existingPayment);
        }

        @Test
        void testUpdatePayment_MonthlyPartialToPartial() {

                Payment existingPayment = new Payment();
                existingPayment.setPaymentId("PAY123");
                existingPayment.setAmount(new BigDecimal("50"));
                existingPayment.setPaymentType(PaymentType.MONTHLY);
                existingPayment.setSubscriptionNumber("SUB123");

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setStatus("PENDING");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);
                allocation.setAmount(new BigDecimal("50"));

                when(paymentRepository.findById("PAY123"))
                                .thenReturn(Optional.of(existingPayment));

                when(paymentAllocationRepository.findByPaymentId("PAY123"))
                                .thenReturn(List.of(allocation));

                when(billRepository.findById(1L))
                                .thenReturn(Optional.of(bill));

                doReturn(PaymentStatus.PARTIAL)
                                .when(paymentService)
                                .applyPaymentEffect(any(Payment.class));

                AddPaymentResponse response = paymentService.updatePayment("PAY123", new BigDecimal("75"));

                assertEquals(new BigDecimal("50"), bill.getBalanceDue());
                assertEquals("PENDING", bill.getStatus());
                assertEquals(PaymentStatus.PARTIAL, response.getStatus());

                verify(paymentRepository).save(existingPayment);
        }

        @Test
        void testUpdatePayment_MonthlyFullToFull() {

                Payment existingPayment = new Payment();
                existingPayment.setPaymentId("PAY123");
                existingPayment.setAmount(new BigDecimal("100"));
                existingPayment.setPaymentType(PaymentType.MONTHLY);
                existingPayment.setSubscriptionNumber("SUB123");

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setTotalAmount(new BigDecimal("100"));
                bill.setStatus("PAID");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);
                allocation.setAmount(new BigDecimal("100"));

                when(paymentRepository.findById("PAY123"))
                                .thenReturn(Optional.of(existingPayment));

                when(paymentAllocationRepository.findByPaymentId("PAY123"))
                                .thenReturn(List.of(allocation));

                when(billRepository.findById(1L))
                                .thenReturn(Optional.of(bill));

                doReturn(PaymentStatus.FULL)
                                .when(paymentService)
                                .applyPaymentEffect(any(Payment.class));

                AddPaymentResponse response = paymentService.updatePayment("PAY123", new BigDecimal("100"));

                assertEquals(new BigDecimal("100"), bill.getBalanceDue());
                assertEquals("PENDING", bill.getStatus());
                assertEquals(PaymentStatus.FULL, response.getStatus());

                verify(paymentRepository).save(existingPayment);
        }

        @Test
        void testUpdatePayment_OutstandingFullToPartial() {

                Payment existingPayment = new Payment();
                existingPayment.setPaymentId("PAY123");
                existingPayment.setAmount(new BigDecimal("300"));
                existingPayment.setPaymentType(PaymentType.OUTSTANDING);
                existingPayment.setSubscriptionNumber("SUB123");

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setStatus("PAID");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);
                allocation.setAmount(new BigDecimal("300"));

                when(paymentRepository.findById("PAY123")).thenReturn(Optional.of(existingPayment));

                when(paymentAllocationRepository.findByPaymentId("PAY123")).thenReturn(List.of(allocation));

                when(billRepository.findById(1L)).thenReturn(Optional.of(bill));

                doReturn(PaymentStatus.PARTIAL)
                                .when(paymentService)
                                .applyPaymentEffect(any(Payment.class));

                AddPaymentResponse response = paymentService.updatePayment("PAY123", new BigDecimal("150"));

                assertEquals(PaymentStatus.PARTIAL, response.getStatus());

                assertEquals(new BigDecimal("150"), existingPayment.getAmount());

                verify(paymentRepository).save(existingPayment);
        }

        @Test
        void testUpdatePayment_OutstandingPartialToFull() {

                Payment existingPayment = new Payment();
                existingPayment.setPaymentId("PAY123");
                existingPayment.setAmount(new BigDecimal("100"));
                existingPayment.setPaymentType(PaymentType.OUTSTANDING);
                existingPayment.setSubscriptionNumber("SUB123");

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBalanceDue(new BigDecimal("200"));
                bill.setStatus("PENDING");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);
                allocation.setAmount(new BigDecimal("100"));

                when(paymentRepository.findById("PAY123")).thenReturn(Optional.of(existingPayment));

                when(paymentAllocationRepository.findByPaymentId("PAY123")).thenReturn(List.of(allocation));

                when(billRepository.findById(1L)).thenReturn(Optional.of(bill));

                doReturn(PaymentStatus.FULL)
                                .when(paymentService)
                                .applyPaymentEffect(any(Payment.class));

                AddPaymentResponse response = paymentService.updatePayment("PAY123", new BigDecimal("300"));

                assertEquals(PaymentStatus.FULL, response.getStatus());

                assertEquals(new BigDecimal("300"), existingPayment.getAmount());

                verify(paymentRepository).save(existingPayment);
        }

        @Test
        void testUpdatePayment_OutstandingPartialToPartial() {

                Payment existingPayment = new Payment();
                existingPayment.setPaymentId("PAY123");
                existingPayment.setAmount(new BigDecimal("100"));
                existingPayment.setPaymentType(PaymentType.OUTSTANDING);
                existingPayment.setSubscriptionNumber("SUB123");

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBalanceDue(new BigDecimal("200"));
                bill.setStatus("PENDING");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);
                allocation.setAmount(new BigDecimal("100"));

                when(paymentRepository.findById("PAY123")).thenReturn(Optional.of(existingPayment));

                when(paymentAllocationRepository.findByPaymentId("PAY123")).thenReturn(List.of(allocation));

                when(billRepository.findById(1L)).thenReturn(Optional.of(bill));

                doReturn(PaymentStatus.PARTIAL)
                                .when(paymentService)
                                .applyPaymentEffect(any(Payment.class));

                AddPaymentResponse response = paymentService.updatePayment("PAY123", new BigDecimal("150"));

                assertEquals(PaymentStatus.PARTIAL, response.getStatus());

                assertEquals(new BigDecimal("150"), existingPayment.getAmount());

                verify(paymentRepository).save(existingPayment);
        }

        @Test
        void testUpdatePayment_OutstandingFullToFull() {

                Payment existingPayment = new Payment();
                existingPayment.setPaymentId("PAY123");
                existingPayment.setAmount(new BigDecimal("300"));
                existingPayment.setPaymentType(PaymentType.OUTSTANDING);
                existingPayment.setSubscriptionNumber("SUB123");

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setStatus("PAID");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);
                allocation.setAmount(new BigDecimal("300"));

                when(paymentRepository.findById("PAY123")).thenReturn(Optional.of(existingPayment));

                when(paymentAllocationRepository.findByPaymentId("PAY123")).thenReturn(List.of(allocation));

                when(billRepository.findById(1L)).thenReturn(Optional.of(bill));

                doReturn(PaymentStatus.FULL)
                                .when(paymentService)
                                .applyPaymentEffect(any(Payment.class));

                AddPaymentResponse response = paymentService.updatePayment("PAY123", new BigDecimal("300"));

                assertEquals(PaymentStatus.FULL, response.getStatus());

                assertEquals(new BigDecimal("300"), existingPayment.getAmount());

                verify(paymentRepository).save(existingPayment);
        }

        @Test
        void testUpdatePayment_InvalidAmount_Zero() {

                Payment payment = new Payment();

                when(paymentRepository.findById("PAY123")).thenReturn(Optional.of(payment));

                assertThrows(InvalidPaymentException.class,
                                () -> paymentService.updatePayment("PAY123", BigDecimal.ZERO));
        }

        @Test
        void testUpdatePayment_InvalidAmount_Negative() {

                Payment payment = new Payment();

                when(paymentRepository.findById("PAY123")).thenReturn(Optional.of(payment));

                assertThrows(InvalidPaymentException.class,
                                () -> paymentService.updatePayment("PAY123", new BigDecimal("-100")));
        }

        @Test
        void testUpdatePayment_InvalidAmount_Null() {

                Payment payment = new Payment();

                when(paymentRepository.findById("PAY123")).thenReturn(Optional.of(payment));

                assertThrows(InvalidPaymentException.class,
                                () -> paymentService.updatePayment("PAY123", null));
        }

        @Test
        void testUpdatePayment_PaymentNotFound() {

                when(paymentRepository.findById("PAY123")).thenReturn(Optional.empty());

                assertThrows(RuntimeException.class,
                                () -> paymentService.updatePayment("PAY123", new BigDecimal("100")));
        }

        @Test
        void testReversePaymentEffect_Success() {

                Payment payment = new Payment();
                payment.setPaymentId("PAY123");

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setStatus("PAID");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);
                allocation.setAmount(new BigDecimal("100"));

                when(paymentAllocationRepository.findByPaymentId("PAY123")).thenReturn(List.of(allocation));

                when(billRepository.findById(1L)).thenReturn(Optional.of(bill));

                paymentService.reversePaymentEffect(payment);

                assertEquals(new BigDecimal("100"), bill.getBalanceDue());

                assertEquals("PENDING", bill.getStatus());

                verify(billRepository).save(bill);

                verify(paymentAllocationRepository).deleteAll(List.of(allocation));
        }

        @Test
        void testReversePaymentEffect_BillNotFound() {

                Payment payment = new Payment();
                payment.setPaymentId("PAY123");

                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setBillId(1L);

                when(paymentAllocationRepository.findByPaymentId("PAY123")).thenReturn(List.of(allocation));

                when(billRepository.findById(1L)).thenReturn(Optional.empty());

                assertThrows(RuntimeException.class,
                                () -> paymentService.reversePaymentEffect(payment));
        }

        @Test
        void testApplyPaymentEffect_Monthly() {

                Payment payment = new Payment();
                payment.setPaymentType(PaymentType.MONTHLY);

                doReturn(PaymentStatus.FULL)
                                .when(paymentService)
                                .reapplyMonthlyPayment(payment);

                PaymentStatus status = paymentService.applyPaymentEffect(payment);

                assertEquals(PaymentStatus.FULL, status);

                verify(paymentService).reapplyMonthlyPayment(payment);
        }

        @Test
        void testApplyPaymentEffect_Outstanding() {

                Payment payment = new Payment();
                payment.setPaymentType(PaymentType.OUTSTANDING);

                doReturn(PaymentStatus.PARTIAL)
                                .when(paymentService)
                                .reapplyOutstandingPayment(payment);

                PaymentStatus status = paymentService.applyPaymentEffect(payment);

                assertEquals(PaymentStatus.PARTIAL, status);

                verify(paymentService).reapplyOutstandingPayment(payment);
        }

        @Test
        void testApplyPaymentEffect_UnsupportedType() {

                Payment payment = new Payment();

                assertThrows(InvalidPaymentException.class,
                                () -> paymentService.applyPaymentEffect(payment));
        }

        @Test
        void testReapplyMonthlyPayment_FullPayment() {

                Payment payment = new Payment();
                payment.setAmount(new BigDecimal("100"));
                payment.setSubscriptionNumber("SUB123");

                Bill latestBill = new Bill();
                latestBill.setBalanceDue(new BigDecimal("100"));
                latestBill.setTotalAmount(new BigDecimal("100"));

                PaymentResult result = new PaymentResult(new BigDecimal("100"), BigDecimal.ZERO, PaymentStatus.FULL);

                doReturn(latestBill)
                                .when(paymentService)
                                .getLatestMonthlyBill("SUB123");

                doReturn(result)
                                .when(paymentService)
                                .processMonthlyPayment(payment, new BigDecimal("100"), latestBill);

                PaymentStatus status = paymentService.reapplyMonthlyPayment(payment);

                assertEquals(PaymentStatus.FULL, status);
        }

        @Test
        void testReapplyMonthlyPayment_PartialPayment() {

                Payment payment = new Payment();
                payment.setAmount(new BigDecimal("70"));
                payment.setSubscriptionNumber("SUB123");

                Bill latestBill = new Bill();
                latestBill.setBalanceDue(new BigDecimal("100"));
                latestBill.setTotalAmount(new BigDecimal("100"));

                PaymentResult result = new PaymentResult(new BigDecimal("70"), new BigDecimal("30"),
                                PaymentStatus.PARTIAL);

                doReturn(latestBill)
                                .when(paymentService)
                                .getLatestMonthlyBill("SUB123");

                doReturn(result)
                                .when(paymentService)
                                .processMonthlyPayment(payment, new BigDecimal("70"), latestBill);

                PaymentStatus status = paymentService.reapplyMonthlyPayment(payment);

                assertEquals(PaymentStatus.PARTIAL, status);
        }

        @Test
        void testReapplyMonthlyPayment_ThrowsExceptionWhenAmountExceedsDue() {
                Payment payment = new Payment();
                payment.setAmount(new BigDecimal("150"));
                payment.setSubscriptionNumber("SUB123");

                Bill latestBill = new Bill();
                latestBill.setBalanceDue(new BigDecimal("100"));
                latestBill.setTotalAmount(new BigDecimal("100"));

                doReturn(latestBill)
                                .when(paymentService).getLatestMonthlyBill("SUB123");

                InvalidPaymentException thrown = assertThrows(InvalidPaymentException.class,
                                () -> paymentService.reapplyMonthlyPayment(payment));

                assertEquals("Payment amount exceeds current balance due", thrown.getMessage());

        }

        @Test
        void testReapplyMonthlyPayment_NullBalanceDue() {
                Payment payment = new Payment();
                payment.setAmount(new BigDecimal("50"));
                payment.setSubscriptionNumber("SUB123");

                Bill latestBill = new Bill();
                latestBill.setBalanceDue(null);
                latestBill.setTotalAmount(new BigDecimal("100"));

                doReturn(latestBill)
                                .when(paymentService)
                                .getLatestMonthlyBill("SUB123");

                InvalidPaymentException thrown = assertThrows(InvalidPaymentException.class,
                                () -> paymentService.reapplyMonthlyPayment(payment));

                assertEquals("Payment amount exceeds current balance due", thrown.getMessage());

        }

        @Test
        void testReapplyOutstandingPayment_FullPayment() {

                Payment payment = new Payment();
                payment.setAmount(new BigDecimal("300"));
                payment.setSubscriptionNumber("SUB123");

                Bill bill1 = new Bill();
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);

                PaymentResult result = new PaymentResult(new BigDecimal("300"), BigDecimal.ZERO, PaymentStatus.FULL);

                doReturn(bills)
                                .when(paymentService)
                                .getOutstandingBillsEntites("SUB123");

                doReturn(result)
                                .when(paymentService)
                                .processOutstandingPayment(payment, new BigDecimal("300"), bills);

                PaymentStatus status = paymentService.reapplyOutstandingPayment(payment);

                assertEquals(PaymentStatus.FULL, status);
        }

        @Test
        void testReapplyOutstandingPayment_PartialPayment() {

                Payment payment = new Payment();
                payment.setAmount(new BigDecimal("150"));
                payment.setSubscriptionNumber("SUB123");

                Bill bill1 = new Bill();
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);

                PaymentResult result = new PaymentResult(new BigDecimal("150"), new BigDecimal("150"),
                                PaymentStatus.PARTIAL);

                doReturn(bills)
                                .when(paymentService)
                                .getOutstandingBillsEntites("SUB123");

                doReturn(result)
                                .when(paymentService)
                                .processOutstandingPayment(payment, new BigDecimal("150"), bills);

                PaymentStatus status = paymentService.reapplyOutstandingPayment(payment);

                assertEquals(PaymentStatus.PARTIAL, status);
        }

        @Test
        void testDeletePayment_Success() {

                Payment payment = new Payment();
                payment.setPaymentId("PAY123");

                when(paymentRepository.findById("PAY123"))
                                .thenReturn(Optional.of(payment));

                doNothing()
                                .when(paymentService)
                                .reversePaymentEffect(payment);

                paymentService.deletePayment("PAY123");

                verify(paymentService).reversePaymentEffect(payment);

                verify(paymentRepository).delete(payment);
        }

        @Test
        void testDeletePayment_NotFound() {

                when(paymentRepository.findById("PAY123"))
                                .thenReturn(Optional.empty());

                assertThrows(RuntimeException.class,
                                () -> paymentService.deletePayment("PAY123"));
        }

        @Test
        void testGetPaymentHistory_InvalidSubscriptionNumber() {

                InvalidPaymentException exception = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.getPaymentHistory(null, 0, 10, null, null));

                assertEquals("Subscription number is required", exception.getMessage());
        }

        @Test
        void testGetPaymentHistory_BlankSubscriptionNumber() {

                InvalidPaymentException exception = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.getPaymentHistory("   ", 0, 10, null, null));

                assertEquals("Subscription number is required", exception.getMessage());
        }

        @Test
        void testGetPaymentHistory_CustomerNotFound() {

                when(customerRepository.existsById("SUB123")).thenReturn(false);

                CustomerNotFoundException exception = assertThrows(
                                CustomerNotFoundException.class,
                                () -> paymentService.getPaymentHistory("SUB123", 0, 10, null, null));

                assertEquals("Customer not found: SUB123", exception.getMessage());
        }

        @Test
        void testGetPaymentHistory_Success() {

                Payment payment = new Payment();
                payment.setPaymentId("PAY123");
                payment.setSubscriptionNumber("SUB123");
                payment.setAmount(new BigDecimal("500"));
                payment.setStatus(PaymentStatus.FULL);
                payment.setPaymentType(PaymentType.MONTHLY);
                payment.setPaymentMethod(PaymentMethod.MANUAL);
                payment.setCreatedAt(LocalDateTime.now());

                List<Payment> paymentList = List.of(payment);

                Page<Payment> paymentPage = new PageImpl<>(paymentList, PageRequest.of(0, 10), 1);

                when(customerRepository.existsById("SUB123"))
                                .thenReturn(true);

                when(paymentRepository.findBySubscriptionNumberAndFilters(
                                eq("SUB123"),
                                anyList(),
                                eq(2025),
                                eq(PaymentMethod.MANUAL),
                                any(Pageable.class)))
                                .thenReturn(paymentPage);

                PaginationResponse<PaymentHistoryItemResponse> response = paymentService.getPaymentHistory(
                                "SUB123", 0, 10, 2025, PaymentMethod.MANUAL);

                assertNotNull(response);

                assertEquals(1, response.getContent().size());

                PaymentHistoryItemResponse item = response.getContent().get(0);

                assertEquals("PAY123", item.getPaymentId());
                assertEquals("SUB123", item.getSubscriptionNumber());
                assertEquals(new BigDecimal("500"), item.getAmount());
                assertEquals(PaymentStatus.FULL, item.getStatus());
                assertEquals(PaymentType.MONTHLY, item.getPaymentType());
                assertEquals(PaymentMethod.MANUAL, item.getPaymentMethod());

                assertEquals(0, response.getCurrentPage());
                assertEquals(1, response.getTotalPages());
                assertEquals(1, response.getTotalElements());
                assertEquals(10, response.getPageSize());
                assertTrue(response.isLast());
        }

        @Test
        void testGetPaymentHistory_EmptyResult() {

                Page<Payment> emptyPage = new PageImpl<>(
                                Collections.emptyList(),
                                PageRequest.of(0, 10),
                                0);

                when(customerRepository.existsById("SUB123"))
                                .thenReturn(true);

                when(paymentRepository.findBySubscriptionNumberAndFilters(
                                eq("SUB123"),
                                anyList(),
                                isNull(),
                                isNull(),
                                any(Pageable.class)))
                                .thenReturn(emptyPage);

                PaginationResponse<PaymentHistoryItemResponse> response = paymentService.getPaymentHistory(
                                "SUB123", 0, 10, null, null);

                assertNotNull(response);
                assertTrue(response.getContent().isEmpty());

                assertEquals(0, response.getTotalElements());
        }

        @Test
        void testGetPaymentHistory_VerifyRepositoryCall() {

                when(customerRepository.existsById("SUB123"))
                                .thenReturn(true);

                when(paymentRepository.findBySubscriptionNumberAndFilters(
                                anyString(),
                                anyList(),
                                any(),
                                any(),
                                any(Pageable.class)))
                                .thenReturn(Page.empty());

                paymentService.getPaymentHistory(
                                "SUB123", 1, 5, 2025, PaymentMethod.MANUAL);

                verify(paymentRepository).findBySubscriptionNumberAndFilters(
                                eq("SUB123"),
                                anyList(),
                                eq(2025),
                                eq(PaymentMethod.MANUAL),
                                argThat(pageable -> pageable.getPageNumber() == 1 &&
                                                pageable.getPageSize() == 5 &&
                                                pageable.getSort().getOrderFor("createdAt") != null));
        }

        @Test
        void testGetCurrentBill_Success() {

                String subscriptionNumber = "SUB123";

                Customer customer = new Customer();
                customer.setSubscriptionNumber(subscriptionNumber);

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setBillingPeriod("2026-05");
                bill.setBillDate(LocalDate.now());
                bill.setTotalAmount(new BigDecimal("1000"));
                bill.setBalanceDue(new BigDecimal("300"));
                bill.setStatus("PARTIAL");

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.of(customer));

                when(billRepository
                                .findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber))
                                .thenReturn(Optional.of(bill));

                CurrentBillResponse response = paymentService.getCurrentBill(subscriptionNumber);

                assertNotNull(response);

                assertEquals(1L, response.getBillId());
                assertEquals("2026-05", response.getBillingPeriod());
                assertEquals(new BigDecimal("1000"), response.getTotalAmount());
                assertEquals(new BigDecimal("700"), response.getAlreadyPaid());
                assertEquals(new BigDecimal("300"), response.getBalanceDue());
                assertEquals("PARTIAL", response.getStatus());
        }

        @Test
        void testGetCurrentBill_CustomerNotFound() {

                String subscriptionNumber = "SUB123";

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.empty());

                RuntimeException exception = assertThrows(CustomerNotFoundException.class,
                                () -> paymentService.getCurrentBill(subscriptionNumber));

                assertEquals("Customer not found: SUB123", exception.getMessage());

                verify(billRepository, never())
                                .findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(anyString());
        }

        @Test
        void testGetCurrentBill_NoBillsFound() {

                String subscriptionNumber = "SUB123";

                Customer customer = new Customer();

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.of(customer));

                when(billRepository
                                .findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber))
                                .thenReturn(Optional.empty());

                RuntimeException exception = assertThrows(RuntimeException.class,
                                () -> paymentService.getCurrentBill(subscriptionNumber));

                assertEquals("No bills found for customer: SUB123", exception.getMessage());
        }

        @Test
        void testGetCurrentBill_NullValues() {

                String subscriptionNumber = "SUB123";

                Customer customer = new Customer();

                Bill bill = new Bill();
                bill.setBillId(1L);
                bill.setTotalAmount(null);
                bill.setBalanceDue(null);
                bill.setStatus(null);

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.of(customer));

                when(billRepository.findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subscriptionNumber))
                                .thenReturn(Optional.of(bill));

                CurrentBillResponse response = paymentService.getCurrentBill(subscriptionNumber);

                assertEquals(BigDecimal.ZERO, response.getTotalAmount());
                assertEquals(BigDecimal.ZERO, response.getAlreadyPaid());
                assertEquals(BigDecimal.ZERO, response.getBalanceDue());

                // default status
                assertEquals("PENDING", response.getStatus());
        }

        @Test
        void testGetOutstandingBills_Success() {

                String subscriptionNumber = "SUB123";

                Customer customer = new Customer();

                Bill bill1 = new Bill();
                bill1.setBillId(1L);
                bill1.setBillingPeriod("2026-03");
                bill1.setBillDate(LocalDate.now());
                bill1.setTotalAmount(new BigDecimal("1000"));
                bill1.setBalanceDue(new BigDecimal("400"));
                bill1.setStatus("PARTIAL");

                Bill bill2 = new Bill();
                bill2.setBillId(2L);
                bill2.setBillingPeriod("2026-04");
                bill2.setBillDate(LocalDate.now());
                bill2.setTotalAmount(new BigDecimal("500"));
                bill2.setBalanceDue(new BigDecimal("500"));
                bill2.setStatus("PENDING");

                List<Bill> bills = List.of(bill1, bill2);

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.of(customer));

                when(billRepository.findOutstandingBillsExcludingLatest(subscriptionNumber))
                                .thenReturn(bills);

                OutstandingBillsSummaryResponse response = paymentService.getOutstandingBills(subscriptionNumber);

                assertNotNull(response);

                // Check total outstanding
                assertEquals(new BigDecimal("900"), response.getTotalOutstandingAmount());

                // Check list size
                assertEquals(2, response.getOutstandingBills().size());

                OutstandingBillResponse first = response.getOutstandingBills().get(0);

                assertEquals(1L, first.getBillId());

                // paid = total - balance
                assertEquals(new BigDecimal("600"), first.getPaidAmount());

                assertEquals(new BigDecimal("400"), first.getBalanceDue());

                assertEquals("PARTIAL", first.getStatus());
        }

        @Test
        void testGetOutstandingBills_NoBills() {

                String subscriptionNumber = "SUB123";

                Customer customer = new Customer();

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.of(customer));

                when(billRepository.findOutstandingBillsExcludingLatest(subscriptionNumber))
                                .thenReturn(List.of());

                OutstandingBillsSummaryResponse response = paymentService.getOutstandingBills(subscriptionNumber);

                assertNotNull(response);

                assertTrue(response.getOutstandingBills().isEmpty());

                assertEquals(BigDecimal.ZERO, response.getTotalOutstandingAmount());
        }

        @Test
        void testGetOutstandingBills_CustomerNotFound() {

                String subscriptionNumber = "SUB123";

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.empty());

                RuntimeException exception = assertThrows(RuntimeException.class,
                                () -> paymentService.getOutstandingBills(subscriptionNumber));

                assertEquals("Customer not found: SUB123", exception.getMessage());

                verify(billRepository, never())
                                .findOutstandingBillsExcludingLatest(anyString());
        }

        @Test
        void testGetOutstandingBills_NullValues() {

                String subscriptionNumber = "SUB123";

                Customer customer = new Customer();

                Bill bill = new Bill();
                bill.setBillId(1L);

                // null values
                bill.setTotalAmount(null);
                bill.setBalanceDue(null);
                bill.setStatus(null);

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.of(customer));

                when(billRepository.findOutstandingBillsExcludingLatest(subscriptionNumber))
                                .thenReturn(List.of(bill));

                OutstandingBillsSummaryResponse response = paymentService.getOutstandingBills(subscriptionNumber);

                OutstandingBillResponse result = response.getOutstandingBills().get(0);

                assertEquals(BigDecimal.ZERO, result.getTotalAmount());
                assertEquals(BigDecimal.ZERO, result.getBalanceDue());
                assertEquals(BigDecimal.ZERO, result.getPaidAmount());

                // default status
                assertEquals("PENDING", result.getStatus());

                assertEquals(BigDecimal.ZERO, response.getTotalOutstandingAmount());
        }

        @Test
        void testGetPaymentCustomerInfo_Success() {

                String subscriptionNumber = "SUB123";

                Region region = new Region();
                region.setRegionName("North");

                Customer customer = new Customer();
                customer.setSubscriptionNumber(subscriptionNumber);
                customer.setAccountHolderName("Kasun Silva");
                customer.setRegion(region);
                customer.setConnectionType("Metered");
                customer.setNic("199945217451V");

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.of(customer));

                PaymentCustomerInfoResponse response = paymentService.getPaymentCustomerInfo(subscriptionNumber);

                assertNotNull(response);

                assertEquals("SUB123", response.getSubscriptionNumber());
                assertEquals("Kasun Silva", response.getAccountHolderName());
                assertEquals("North", response.getRegion());
                assertEquals("Metered", response.getConnectionType());
                assertEquals("199945217451V", response.getNic());
        }

        @Test
        void testGetPaymentCustomerInfo_CustomerNotFound() {

                String subscriptionNumber = "SUB123";

                when(customerRepository.findById(subscriptionNumber))
                                .thenReturn(Optional.empty());

                RuntimeException exception = assertThrows(RuntimeException.class,
                                () -> paymentService.getPaymentCustomerInfo(subscriptionNumber));

                assertEquals("Customer not found: SUB123", exception.getMessage());
        }

        @Test
        void testGetRecentPayments_Success() {

                int limit = 5;

                Payment payment = new Payment();
                payment.setPaymentId("PAY123");
                payment.setSubscriptionNumber("SUB123");
                payment.setAmount(new BigDecimal("1500"));
                payment.setStatus(PaymentStatus.PARTIAL);
                payment.setCreatedAt(LocalDateTime.now());
                payment.setPaymentMethod(PaymentMethod.MANUAL);
                payment.setPaymentType(PaymentType.MONTHLY);

                Customer customer = new Customer();
                customer.setSubscriptionNumber("SUB123");
                customer.setAccountHolderName("Kasun Silva");

                when(paymentRepository.findByPaymentMethodInOrderByCreatedAtDesc(anyList(), any(Pageable.class)))
                                .thenReturn(List.of(payment));

                when(customerRepository.findBySubscriptionNumber("SUB123"))
                                .thenReturn(Optional.of(customer));

                List<RecentPaymentResponse> response = paymentService.getRecentPayments(limit);

                assertNotNull(response);

                assertEquals(1, response.size());

                RecentPaymentResponse result = response.get(0);

                assertEquals("PAY123", result.getPaymentId());
                assertEquals("SUB123", result.getSubscriptionNumber());
                assertEquals(new BigDecimal("1500"), result.getAmountPaid());
                assertEquals("PARTIAL", result.getStatus());
                assertEquals("Kasun Silva", result.getAccountHolderName());
                assertEquals(PaymentMethod.MANUAL, result.getPaymentMethod());
                assertEquals(PaymentType.MONTHLY, result.getPaymentType());
        }

        @Test
        void testGetRecentPayments_CustomerNotFound() {

                int limit = 5;

                Payment payment = new Payment();
                payment.setPaymentId("PAY123");
                payment.setSubscriptionNumber("SUB999");
                payment.setAmount(new BigDecimal("1000"));
                payment.setStatus(PaymentStatus.PARTIAL);
                payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

                when(paymentRepository.findByPaymentMethodInOrderByCreatedAtDesc(anyList(), any(Pageable.class)))
                                .thenReturn(List.of(payment));

                when(customerRepository.findBySubscriptionNumber("SUB999"))
                                .thenReturn(Optional.empty());

                List<RecentPaymentResponse> response = paymentService.getRecentPayments(limit);

                assertEquals(1, response.size());

                RecentPaymentResponse result = response.get(0);

                // customer not found
                assertEquals("Unknown", result.getAccountHolderName());
        }

        @Test
        void testGetRecentPayments_EmptyList() {

                int limit = 5;

                when(paymentRepository.findByPaymentMethodInOrderByCreatedAtDesc(anyList(), any(Pageable.class)))
                                .thenReturn(List.of());

                List<RecentPaymentResponse> response = paymentService.getRecentPayments(limit);

                assertNotNull(response);
                assertTrue(response.isEmpty());
        }

        @Test
        void testGetRecentPayments_VerifyRepositoryCall() {

                int limit = 10;

                when(paymentRepository.findByPaymentMethodInOrderByCreatedAtDesc(anyList(), any(Pageable.class)))
                                .thenReturn(List.of());

                paymentService.getRecentPayments(limit);

                verify(paymentRepository)
                                .findByPaymentMethodInOrderByCreatedAtDesc(
                                                eq(List.of(PaymentMethod.MANUAL, PaymentMethod.BANK_TRANSFER)),
                                                any(Pageable.class));
        }

        @Test
        void testGetCustomerPaymentSummary_NullSubscriptionNumber() {

                InvalidPaymentException exception = assertThrows(InvalidPaymentException.class,
                                () -> paymentService.getCustomerPaymentSummary(null));

                assertEquals("Subscription number is required", exception.getMessage());
        }

        @Test
        void testGetCustomerPaymentSummary_BlankSubscriptionNumber() {

                InvalidPaymentException exception = assertThrows(InvalidPaymentException.class,
                                () -> paymentService.getCustomerPaymentSummary(" "));

                assertEquals("Subscription number is required", exception.getMessage());
        }

        @Test
        void testGetCustomerPaymentSummary_CustomerNotFound() {

                when(customerRepository.findById("SUB123")).thenReturn(Optional.empty());

                CustomerNotFoundException exception = assertThrows(CustomerNotFoundException.class,
                                () -> paymentService.getCustomerPaymentSummary("SUB123"));

                assertEquals("Customer not found: SUB123", exception.getMessage());
        }

        @Test
        void testGetCustomerPaymentSummary_WithOutstandingBill() {

                Customer customer = new Customer();
                customer.setSubscriptionNumber("SUB123");
                customer.setOutstandingBalance(new BigDecimal("200"));

                Bill bill = new Bill();
                bill.setBalanceDue(new BigDecimal("500"));
                bill.setStatus("PENDING");

                when(customerRepository.findById("SUB123"))
                                .thenReturn(Optional.of(customer));

                when(billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc("SUB123"))
                                .thenReturn(List.of(bill));

                CustomerPaymentSummaryResponse response = paymentService.getCustomerPaymentSummary("SUB123");

                assertEquals("SUB123", response.getSubscriptionNumber());
                assertEquals(new BigDecimal("500"), response.getMonthlyDue());
                assertEquals(new BigDecimal("200"), response.getOutstandingBalance());
                assertEquals(new BigDecimal("700"), response.getTotalDue());
                assertEquals("PENDING", response.getBillStatus());
        }

        @Test
        void testGetCustomerPaymentSummary_AllBillsPaid() {

                Customer customer = new Customer();
                customer.setSubscriptionNumber("SUB123");
                customer.setOutstandingBalance(new BigDecimal("100"));

                Bill bill = new Bill();
                bill.setBalanceDue(BigDecimal.ZERO);
                bill.setStatus("PAID");

                when(customerRepository.findById("SUB123"))
                                .thenReturn(Optional.of(customer));

                when(billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc("SUB123"))
                                .thenReturn(List.of(bill));

                CustomerPaymentSummaryResponse response = paymentService.getCustomerPaymentSummary("SUB123");

                assertEquals(BigDecimal.ZERO, response.getMonthlyDue());
                assertEquals(new BigDecimal("100"), response.getOutstandingBalance());
                assertEquals(new BigDecimal("100"), response.getTotalDue());
                assertEquals("NO_BILL", response.getBillStatus());
        }

        @Test
        void testGetCustomerPaymentSummary_NullOutstandingBalance() {

                Customer customer = new Customer();
                customer.setSubscriptionNumber("SUB123");
                customer.setOutstandingBalance(null);

                when(customerRepository.findById("SUB123"))
                                .thenReturn(Optional.of(customer));

                when(billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc("SUB123"))
                                .thenReturn(Collections.emptyList());

                CustomerPaymentSummaryResponse response = paymentService.getCustomerPaymentSummary("SUB123");

                assertEquals(BigDecimal.ZERO, response.getOutstandingBalance());
                assertEquals(BigDecimal.ZERO, response.getTotalDue());
                assertEquals("NO_BILL", response.getBillStatus());
        }

        @Test
        void testGetCustomerPaymentSummary_NullBillStatus() {

                Customer customer = new Customer();
                customer.setOutstandingBalance(new BigDecimal("50"));

                Bill bill = new Bill();
                bill.setBalanceDue(new BigDecimal("300"));
                bill.setStatus(null);

                when(customerRepository.findById("SUB123"))
                                .thenReturn(Optional.of(customer));

                when(billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc("SUB123"))
                                .thenReturn(List.of(bill));

                CustomerPaymentSummaryResponse response = paymentService.getCustomerPaymentSummary("SUB123");

                assertEquals(new BigDecimal("300"), response.getMonthlyDue());
                assertEquals("PENDING", response.getBillStatus());
        }

        @Test
        void testGetCustomerPaymentSummary_SelectFirstUnpaidBill() {

                Customer customer = new Customer();
                customer.setOutstandingBalance(BigDecimal.ZERO);

                Bill paidBill = new Bill();
                paidBill.setBalanceDue(BigDecimal.ZERO);
                paidBill.setStatus("PAID");

                Bill unpaidBill = new Bill();
                unpaidBill.setBalanceDue(new BigDecimal("400"));
                unpaidBill.setStatus("PENDING");

                when(customerRepository.findById("SUB123"))
                                .thenReturn(Optional.of(customer));

                when(billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc("SUB123"))
                                .thenReturn(List.of(paidBill, unpaidBill));

                CustomerPaymentSummaryResponse response = paymentService.getCustomerPaymentSummary("SUB123");

                assertEquals(new BigDecimal("400"), response.getMonthlyDue());
                assertEquals("PENDING", response.getBillStatus());
        }

}
