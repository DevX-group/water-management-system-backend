package com.backend.water_management_system.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.common.entity.ConnectionRate;

@Repository
public interface RateRepository extends JpaRepository<ConnectionRate, String> {
}