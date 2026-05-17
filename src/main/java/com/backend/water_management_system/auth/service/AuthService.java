package com.backend.water_management_system.auth.service;

import com.backend.water_management_system.auth.dto.ActivationRequest;
import com.backend.water_management_system.auth.dto.LoginRequest;
import com.backend.water_management_system.auth.dto.LoginResponse;
import com.backend.water_management_system.security.JwtService;
import com.backend.water_management_system.security.UserPrincipal;
import com.backend.water_management_system.user.entity.ActivationToken;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.ActivationTokenRepository;
import com.backend.water_management_system.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.activation-token-expiry-hours:72}")
    private int activationTokenExpiryHours;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository,
                       ActivationTokenRepository activationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       MailSender mailSender) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public LoginResponse login(LoginRequest request) {
        // AuthenticationManager handles credential validation + account status checks
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.nic(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String token = jwtService.generateToken(user.getNic(), user.getRole().name());

        return new LoginResponse(token, user.getRole().name(), user.getNic(), user.getEmail());
    }

    // ── Account activation ────────────────────────────────────────────────────

    // Generates an activation token for the given user and sends it via email.
    // Any previously unused token for this user is invalidated first.
    @Transactional
    public void sendActivationLink(User user) {
        // Invalidate any existing unused token for this user
        activationTokenRepository.findByUserAndUsedFalse(user)
                .ifPresent(existing -> {
                    existing.setUsed(true);
                    activationTokenRepository.save(existing);
                });

        // Create a new token
        String tokenValue = UUID.randomUUID().toString();
        ActivationToken activationToken = ActivationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(activationTokenExpiryHours))
                .used(false)
                .build();
        activationTokenRepository.save(activationToken);

        // Send activation email
        sendActivationEmail(user.getEmail(), tokenValue);
    }

    // Validates the activation token, sets the user's password, and marks the account as active.
    @Transactional
    public void activateAccount(ActivationRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        ActivationToken activationToken = activationTokenRepository
                .findByToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid activation link"));

        if (activationToken.isUsed()) {
            throw new IllegalStateException("This activation link has already been used");
        }

        if (activationToken.isExpired()) {
            throw new IllegalStateException("This activation link has expired. Please request a new one");
        }

        User user = activationToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        activationToken.setUsed(true);
        activationTokenRepository.save(activationToken);
    }

    // ── Email ─────────────────────────────────────────────────────────────────

    private void sendActivationEmail(String toEmail, String token) {
        String activationLink = frontendUrl + "/activate?token=" + token;

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromEmail);
        mail.setTo(toEmail);
        mail.setSubject("Activate your HydroPay account");
        mail.setText(
                "Welcome to HydroPay!\n\n" +
                "Please click the link below to activate your account and set your password.\n" +
                "This link is valid for " + activationTokenExpiryHours + " hours.\n\n" +
                activationLink + "\n\n" +
                "If you did not expect this email, please ignore it.\n\n" +
                "HydroPay Water Management System"
        );

        mailSender.send(mail);
    }
}
