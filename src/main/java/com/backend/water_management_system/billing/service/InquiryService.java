package com.backend.water_management_system.billing.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.water_management_system.billing.entity.Inquiry;
import com.backend.water_management_system.billing.entity.InquiryMessage;
import com.backend.water_management_system.billing.repository.InquiryRepository;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    public InquiryService(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
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

    public List<Inquiry> getInquiriesByEmail(String email) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}