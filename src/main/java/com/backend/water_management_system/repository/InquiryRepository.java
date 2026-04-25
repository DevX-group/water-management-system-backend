package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, String> {
    // JpaRepository provides save(), findAll(), and findById() automatically
}