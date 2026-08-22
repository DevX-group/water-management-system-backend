package com.backend.water_management_system.inquiry.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.inquiry.entity.Inquiry;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, String> {
    // JpaRepository provides save(), findAll(), and findById() automatically

    /** Count inquiries by status string (e.g. "open", "pending", "resolved"). */
    long countByStatus(String status);

    /** Count inquiries by email (customer-scoped). */
    long countByEmailAndStatus(String email, String status);

    List<Inquiry> findByEmail(String email);
}