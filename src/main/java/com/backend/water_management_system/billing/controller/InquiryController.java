package com.backend.water_management_system.billing.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.billing.entity.Inquiry;
import com.backend.water_management_system.billing.entity.InquiryMessage;
import com.backend.water_management_system.billing.service.InquiryService;

@RestController
@RequestMapping("/api/inquiries")
@CrossOrigin(origins = "*") 
public class InquiryController {

    @Autowired
    private InquiryService inquiryService; // Use the service instead of repository

    @PostMapping
    public Inquiry createInquiry(@RequestBody Inquiry inquiry) {
        return inquiryService.createInquiry(inquiry);
    }

    @GetMapping
    public List<Inquiry> getAllInquiries() {
        return inquiryService.getAllInquiries();
    }

    @PostMapping("/{id}/messages")
    public Inquiry addMessage(@PathVariable String id, @RequestBody InquiryMessage message) {
        return inquiryService.addMessage(id, message);
    }

    @PatchMapping("/{id}/status")
    public Inquiry updateStatus(@PathVariable String id, @RequestParam String status) {
        return inquiryService.updateStatus(id, status);
    }
}