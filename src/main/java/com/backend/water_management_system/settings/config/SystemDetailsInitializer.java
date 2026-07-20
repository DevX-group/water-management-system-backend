package com.backend.water_management_system.settings.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.backend.water_management_system.settings.entity.SystemDetails;
import com.backend.water_management_system.settings.repository.SystemDetailsRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SystemDetailsInitializer implements CommandLineRunner {

    private final SystemDetailsRepository systemDetailsRepository;

    @Override
    public void run(String... args) {

        if (systemDetailsRepository.count() == 0) {

            SystemDetails details = new SystemDetails();

            details.setCreatedAt(LocalDateTime.now());
            details.setUpdatedAt(LocalDateTime.now());

            details.setCompanyName("Water Management System");
            details.setDefaultCurrency("LKR");

            systemDetailsRepository.save(details);
        }
    }
}