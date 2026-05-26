package com.backend.water_management_system.payments.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.messaging.service.TriggeredMessageDispatcher;
import com.backend.water_management_system.payments.dto.AddPaymentRequest;
import com.backend.water_management_system.payments.dto.AddPaymentResponse;
import com.backend.water_management_system.payments.dto.PaymentResult;
import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.entity.PaymentAllocation;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentStatus;
import com.backend.water_management_system.payments.enums.PaymentType;
import com.backend.water_management_system.payments.exceptions.InvalidPaymentException;
import com.backend.water_management_system.payments.repository.PaymentAllocationRepository;
import com.backend.water_management_system.payments.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

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
        @InjectMocks
        private PaymentService paymentService;

        @Test
        public void testLatestMonthlyBill_Success() {
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
        public void testLatestMonthlyBill_ThrowsExceptionWhenNotFound() {
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
        public void testOutstandingBills_Success() {
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
        public void testOutstandingBills_ThrowsExceptionWhenNotFound() {
                String testSubscriptionNumber = "SUB123";

                when(billRepository.findOutstandingBillsExcludingLatest(testSubscriptionNumber))
                                .thenReturn(List.of()); // creates an empty list

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.getOutstandingBillsEntites(testSubscriptionNumber));

                assertEquals("No outstanding bills found", thrown.getMessage());
        }

        @Test
        public void testValidateRequest_ThrowsExceptionIfSubscriptionNumberIsNull() {
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
        public void testValidateRequest_ThrowsExceptionIfSubscriptionNumberIsBlank() {
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
        public void testValidateRequest_ThrowsExceptionIfAmountIsZero() {
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
        public void testValidateRequest_ThrowsExceptionIfAmountIsNegative() {
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
        public void testValidateRequest_ThrowsExceptionIfPaymentTypeIsNull() {
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
        public void testValidateRequest_ThrowsExceptionIfPaymentMethodIsNull() {
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
        public void testValidateRequest_Success() {
                AddPaymentRequest validRequest = new AddPaymentRequest();
                validRequest.setSubscriptionNumber("SUB123");
                validRequest.setAmount(new BigDecimal("100.00"));
                validRequest.setPaymentType(PaymentType.MONTHLY);
                validRequest.setPaymentMethod(PaymentMethod.MANUAL);

                paymentService.validateRequest(validRequest);
        }

        @Test
        public void testValidateMonthlyAmount_Success() {
                Bill bill = new Bill();
                bill.setBalanceDue(new BigDecimal("100"));

                paymentService.validateMonthlyPayment(new BigDecimal("50"), bill);

        }

        @Test
        public void testValidateMonthlyAmount_ThrowsExceptionIfAmountIsGreaterThanBalanceDue() {
                Bill bill = new Bill();
                bill.setBalanceDue(new BigDecimal("100"));

                InvalidPaymentException thrown = assertThrows(
                                InvalidPaymentException.class,
                                () -> paymentService.validateMonthlyPayment(new BigDecimal("150"), bill));
                assertEquals("Amount cannot be greater than monthly due", thrown.getMessage());
        }

        @Test
        public void testValidateOutstandingAmount_Success() {
                Bill bill1 = new Bill();
                bill1.setBalanceDue(new BigDecimal("100"));

                Bill bill2 = new Bill();
                bill2.setBalanceDue(new BigDecimal("200"));

                List<Bill> bills = List.of(bill1, bill2);
                paymentService.validateOutstandingPayment(new BigDecimal("50"), bills);
        }

        @Test
        public void testValidateOutstandingAmount_ThrowsExceptionIfAmountIsGreaterThanTotalOutstandingAmount() {
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
        public void testCreatePaymentEntity_Success() {
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
        public void testProcessMonthlyPayment_FullPayment() {

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
        public void testProcessMonthlyPayment_PartialPayment() {

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
        public void testProcessMonthlyPayment_FullyClearsExistingPartialBill() {

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
        public void testProcessOutstandingPayment_PartialPaymentAcrossBills() {

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
        public void testProcessOutstandingPayment_FullPayment() {
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
        public void testProcessOutstandingPayment_FullyClearsExistingPartialBills() {
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

                // Execute method
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
        public void testAddPayment_MonthlyFullPaymentSuccess() {

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
        public void testAddPayment_MonthlyPartialPaymentSuccess() {

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
        public void testAddPayment_MonthlyClearsExistingPartialBill() {

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
        public void testAddPayment_OutstandingFullPaymentSuccess() {

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
        public void testAddPayment_OutstandingPartialPaymentSuccess() {

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
        public void testAddPayment_OutstandingClearsExistingPartialBills() {

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
        public void testAddPayment_ThrowsExceptionWhenCustomerNotFound() {

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
        public void testAddPayment_ThrowsExceptionWhenNoMonthlyBillFound() {

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
        public void testAddPayment_ThrowsExceptionWhenNoOutstandingBillsFound() {

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
        public void testAddPayment_DoesNotDispatchMessageForBankTransfer() {

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
}
