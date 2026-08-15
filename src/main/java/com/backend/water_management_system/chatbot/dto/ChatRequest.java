package com.backend.water_management_system.chatbot.dto;

public class ChatRequest {
    private String message;
    private String subscriptionNumber;

    public ChatRequest() {}

    public ChatRequest(String message, String subscriptionNumber) {
        this.message = message;
        this.subscriptionNumber = subscriptionNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSubscriptionNumber() {
        return subscriptionNumber;
    }

    public void setSubscriptionNumber(String subscriptionNumber) {
        this.subscriptionNumber = subscriptionNumber;
    }
}
