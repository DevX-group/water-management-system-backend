package com.backend.water_management_system.chatbot.controller;

import com.backend.water_management_system.chatbot.dto.ChatRequest;
import com.backend.water_management_system.chatbot.dto.ChatResponse;
import com.backend.water_management_system.chatbot.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/ask")
    public ChatResponse askChatbot(@RequestBody ChatRequest request) {
        String answer = chatbotService.getChatbotResponse(request.getMessage(), request.getSubscriptionNumber());
        return new ChatResponse(answer);
    }
}
