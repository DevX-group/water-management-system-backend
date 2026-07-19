package com.backend.water_management_system.chatbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.chatbot.dto.ChatbotRequest;
import com.backend.water_management_system.chatbot.dto.ChatbotResponse;
import com.backend.water_management_system.chatbot.service.ChatbotService;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatbotResponse> ask(@RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.ask(request);
        return ResponseEntity.ok(response);
    }
}
