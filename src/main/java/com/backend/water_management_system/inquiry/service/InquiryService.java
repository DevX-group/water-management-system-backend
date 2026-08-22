package com.backend.water_management_system.inquiry.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.water_management_system.inquiry.entity.Inquiry;
import com.backend.water_management_system.inquiry.entity.InquiryMessage;
import com.backend.water_management_system.inquiry.repository.InquiryRepository;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final com.backend.water_management_system.user.repository.UserRepository userRepository;

    public InquiryService(InquiryRepository inquiryRepository,
                          com.backend.water_management_system.user.repository.UserRepository userRepository) {
        this.inquiryRepository = inquiryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Saves a new inquiry thread started by a customer
     */
    @Transactional
    public Inquiry createInquiry(Inquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }

    /**
     * Retrieves all inquiries for the admin dashboard
     */
    public List<Inquiry> getAllInquiries() {
        return inquiryRepository.findAll();
    }

    /**
     * Adds a new message to an existing conversation
     */
    @Transactional
    public Inquiry addMessage(String inquiryId, InquiryMessage newMessage) {
        return inquiryRepository.findById(inquiryId)
            .map(inquiry -> {
                inquiry.getMessages().add(newMessage);
                return inquiryRepository.save(inquiry);
            })
            .orElseThrow(() -> new RuntimeException("Inquiry not found with id: " + inquiryId));
    }

    /**
     * Updates the ticket status (e.g., 'open' to 'resolved')
     */
    @Transactional
    public Inquiry updateStatus(String inquiryId, String status) {
        return inquiryRepository.findById(inquiryId)
            .map(inquiry -> {
                inquiry.setStatus(status);
                return inquiryRepository.save(inquiry);
            })
            .orElseThrow(() -> new RuntimeException("Inquiry not found with id: " + inquiryId));
    }

    public List<Inquiry> getInquiriesForCustomer(String nic) {
        com.backend.water_management_system.user.entity.User user = userRepository.findByNic(nic)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return inquiryRepository.findByEmail(user.getEmail());
    }
}