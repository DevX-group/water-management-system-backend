package com.backend.water_management_system.user.config;

import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Runs at startup. If no SUPER_ADMIN exists yet, creates one using credentials from the environment.
// @Order(1) ensures this runs before the existing DataSeeder.
@Component
@Order(1)
public class SuperAdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.nic}")
    private String superAdminNic;

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    public SuperAdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
