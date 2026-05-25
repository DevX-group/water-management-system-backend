package com.backend.water_management_system.payments.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.messaging.service.TriggeredMessageDispatcher;
import com.backend.water_management_system.payments.dto.AddPaymentRequest;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentType;
import com.backend.water_management_system.payments.exceptions.InvalidPaymentException;
import com.backend.water_management_system.payments.repository.PaymentAllocationRepository;
import com.backend.water_management_system.payments.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

        // 1. Mock all dependencies found in the PaymentService constructor
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

        // 2. Inject mocks into the service we are testing
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
        public void testValidateRequest_ThrowsExceptionIfAmountIsZero() {
                // Create a request with an invalid amount (ZERO)
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
}
