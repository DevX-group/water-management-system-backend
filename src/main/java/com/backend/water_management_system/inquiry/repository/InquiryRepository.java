package com.backend.water_management_system.inquiry.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.inquiry.entity.Inquiry;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, String> {
    long countByStatus(String status);
    long countByEmailAndStatus(String email, String status);

    List<Inquiry> findByEmail(String email);
    java.util.List<Inquiry> findByEmail(String email);
    org.springframework.data.domain.Page<Inquiry> findByEmail(String email, org.springframework.data.domain.Pageable pageable);
}