package com.backend.water_management_system.chatbot.service;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.billing.repository.BillRepository;
import com.backend.water_management_system.chatbot.dto.ChatbotRequest;
import com.backend.water_management_system.chatbot.dto.ChatbotResponse;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ChatbotService {

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;

    public ChatbotService(BillRepository billRepository, CustomerRepository customerRepository) {
        this.billRepository = billRepository;
        this.customerRepository = customerRepository;
    }

    public ChatbotResponse ask(ChatbotRequest request) {
        String message = request.getMessage().toLowerCase();
        String subNumber = request.getSubscriptionNumber();

        if (subNumber == null || subNumber.isEmpty()) {
            return new ChatbotResponse("Please log in or provide your subscription number so I can check your details.");
        }

        Optional<Customer> customerOpt = customerRepository.findById(subNumber);
        if (customerOpt.isEmpty()) {
            return new ChatbotResponse("I couldn't find a customer with subscription number " + subNumber + ".");
        }
        
        Customer customer = customerOpt.get();

        if (message.contains("hi") || message.contains("hello")) {
            return new ChatbotResponse("Hello " + customer.getAccountHolderName() + "! How can I assist you with your water bills today?");
        }

        if (message.contains("reduce") || message.contains("save")) {
            return new ChatbotResponse("To reduce your water bill, try fixing leaks immediately, taking shorter showers, and turning off the tap while brushing teeth. Small changes make a big difference!");
        }

        if (message.contains("this month")) {
            Optional<Bill> latestBill = billRepository.findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subNumber);
            if (latestBill.isPresent()) {
                Bill bill = latestBill.get();
                return new ChatbotResponse("Your bill for this month (" + bill.getBillingPeriod() + ") is LKR " + bill.getTotalAmount().setScale(2) + ". The usage was " + bill.getUsageUnits() + " units.");
            }
            return new ChatbotResponse("You don't have a bill generated for this month yet.");
        }

        if (message.contains("last month")) {
            // Find all bills, ordered by date desc. The second one is usually last month's.
            var bills = billRepository.findByCustomer_SubscriptionNumberOrderByBillDateDesc(subNumber);
            if (bills.size() > 1) {
                Bill lastMonthBill = bills.get(1);
                return new ChatbotResponse("Your usage for last month (" + lastMonthBill.getBillingPeriod() + ") was " + lastMonthBill.getUsageUnits() + " units, and the bill was LKR " + lastMonthBill.getTotalAmount().setScale(2) + ".");
            }
            return new ChatbotResponse("I couldn't find a bill for last month.");
        }

        if (message.contains("bill") || message.contains("due") || message.contains("amount") || message.contains("balance") || message.contains("pay")) {
            // Get the total pending balance
            BigDecimal pendingBalance = billRepository.getTotalPendingBalance(subNumber);
            
            // Get the latest bill to show details
            Optional<Bill> latestBill = billRepository.findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subNumber);
            
            if (latestBill.isPresent()) {
                Bill bill = latestBill.get();
                if (pendingBalance.compareTo(BigDecimal.ZERO) > 0) {
                    return new ChatbotResponse("Your total outstanding balance is LKR " + pendingBalance.setScale(2) + ". Your last bill was generated on " + bill.getBillDate() + " for LKR " + bill.getTotalAmount().setScale(2) + ". The due date is " + bill.getDueDate() + ".");
                } else {
                    return new ChatbotResponse("Great news! You have no outstanding balance. Your last bill of LKR " + bill.getTotalAmount().setScale(2) + " has been settled.");
                }
            } else {
                return new ChatbotResponse("You don't have any bills generated yet.");
            }
        }

        if (message.contains("usage") || message.contains("units")) {
            Optional<Bill> latestBill = billRepository.findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(subNumber);
            if (latestBill.isPresent() && latestBill.get().getUsageUnits() != null) {
                return new ChatbotResponse("Your last recorded usage was " + latestBill.get().getUsageUnits() + " units for the billing period " + latestBill.get().getBillingPeriod() + ".");
            } else {
                return new ChatbotResponse("I couldn't find any recent usage records for your account.");
            }
        }

        return new ChatbotResponse("I'm a simple AI assistant. You can ask me about your 'bill', 'outstanding balance', 'last month usage', or 'how to reduce bill'.");
    }
}
