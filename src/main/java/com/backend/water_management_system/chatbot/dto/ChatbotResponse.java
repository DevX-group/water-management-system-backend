package com.backend.water_management_system.chatbot.dto;

public class ChatbotResponse {
    private String response;

    public ChatbotResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
