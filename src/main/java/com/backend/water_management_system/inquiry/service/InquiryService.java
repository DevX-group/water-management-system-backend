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

    @Transactional
    public Inquiry createInquiry(Inquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }
    public List<Inquiry> getAllInquiries() {
        return inquiryRepository.findAll();
    }
    @Transactional
    public Inquiry addMessage(String inquiryId, InquiryMessage newMessage) {
        return inquiryRepository.findById(inquiryId)
            .map(inquiry -> {
                inquiry.getMessages().add(newMessage);
                return inquiryRepository.save(inquiry);
            })
            .orElseThrow(() -> new RuntimeException("Inquiry not found with id: " + inquiryId));
    }
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

    public org.springframework.data.domain.Page<Inquiry> getAllInquiriesPaginated(org.springframework.data.domain.Pageable pageable) {
        return inquiryRepository.findAll(pageable);
    }

    public org.springframework.data.domain.Page<Inquiry> getInquiriesForCustomerPaginated(String nic, org.springframework.data.domain.Pageable pageable) {
        com.backend.water_management_system.user.entity.User user = userRepository.findByNic(nic)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return inquiryRepository.findByEmail(user.getEmail(), pageable);
    }
}