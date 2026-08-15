package com.backend.water_management_system.user.config;

import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Runs on startup — creates the first SUPER_ADMIN if one doesn't exist yet.
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SuperAdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.nic}")
    private String superAdminNic;

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            log.info("Super admin already exists, skipping bootstrap.");
            return;
        }

        User superAdmin = User.builder()
                .nic(superAdminNic)
                .email(superAdminEmail)
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(superAdmin);
        log.info("Default super admin created with NIC: {}", superAdminNic);
    }
}
