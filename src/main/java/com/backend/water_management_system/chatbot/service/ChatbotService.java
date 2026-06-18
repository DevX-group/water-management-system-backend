package com.backend.water_management_system.chatbot.service;

import io.github.cdimascio.dotenv.Dotenv;
import com.backend.water_management_system.billing.service.BillService;
import com.backend.water_management_system.billing.dto.BillResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final BillService billService;

    public ChatbotService(BillService billService) {
        this.restTemplate = new RestTemplate();
        this.billService = billService;
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.apiKey = dotenv.get("GEMINI_API_KEY", "");
    }

    public String getChatbotResponse(String userMessage, String subscriptionNumber) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your_api_key_here")) {
            return "Server Error: Gemini API Key is not configured. Please add it to the .env file.";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey.trim();

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are a helpful, professional, and friendly Water Management Assistant. ");
        systemPrompt.append("Your job is to help customers of the local Water Board. ");
        systemPrompt.append("You can answer general questions about water bills, how to reduce water usage, how to report leaks, and other related topics. ");
        systemPrompt.append("Keep your answers concise, clear, and polite. ");

        // Append personalized billing data if available
        if (subscriptionNumber != null && !subscriptionNumber.isEmpty()) {
            try {
                List<BillResponse> bills = billService.getBillsForCustomer(subscriptionNumber);
                if (bills != null && !bills.isEmpty()) {
                    systemPrompt.append("\n\n--- PERSONALIZED CUSTOMER DATA ---\n");
                    systemPrompt.append("The customer talking to you currently has the Subscription Number: ").append(subscriptionNumber).append(".\n");
                    systemPrompt.append("Here is their recent billing history from the database:\n");
                    for (BillResponse bill : bills) {
                        systemPrompt.append("- Bill Date: ").append(bill.billDate)
                                    .append(", Due Date: ").append(bill.dueDate)
                                    .append(", Total Amount: LKR ").append(bill.totalAmount)
                                    .append(", Balance Due: LKR ").append(bill.balanceDue)
                                    .append(", Status: ").append(bill.status)
                                    .append(", Usage: ").append(bill.usageUnits).append(" units.\n");
                    }
                    systemPrompt.append("Use this precise data to answer any questions they have about their bill, due dates, or amounts owed. Do not guess.");
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch bills for chatbot context: " + e.getMessage());
            }
        }

        Map<String, Object> requestBody = Map.of(
            "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt.toString()))),
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", userMessage)))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(URI.create(url), entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            return "I'm sorry, I couldn't process your request right now. Please try again later.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Server error while communicating with Gemini API. Please check your API key and backend logs.";
        }
    }
}
