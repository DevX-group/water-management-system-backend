package com.backend.water_management_system.inquiry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.inquiry.entity.Inquiry;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, String> {
    List<Inquiry> findByEmailOrderByCreatedAtDesc(String email);
}