package com.backend.water_management_system.controller;

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

import com.backend.water_management_system.entity.Inquiry;
import com.backend.water_management_system.entity.InquiryMessage;
import com.backend.water_management_system.repository.InquiryRepository;

@RestController
@RequestMapping("/api/inquiries")
@CrossOrigin(origins = "*") 
public class InquiryController {

    @Autowired
    private InquiryRepository inquiryRepository;

    @PostMapping
    public Inquiry createInquiry(@RequestBody Inquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }

    @GetMapping
    public List<Inquiry> getAllInquiries() {
        return inquiryRepository.findAll();
    }

    @PostMapping("/{id}/messages")
    public Inquiry addMessage(@PathVariable String id, @RequestBody InquiryMessage message) {
        Inquiry inquiry = inquiryRepository.findById(id).orElseThrow();
        inquiry.getMessages().add(message);
        return inquiryRepository.save(inquiry);
    }

    @PatchMapping("/{id}/status")
    public Inquiry updateStatus(@PathVariable String id, @RequestParam String status) {
        Inquiry inquiry = inquiryRepository.findById(id).orElseThrow();
        inquiry.setStatus(status);
        return inquiryRepository.save(inquiry);
    }
}