package com.backend.water_management_system.security;

import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "0123456789012345678901234567890123456789012345678901234567890123";

    @AfterEach
    void clearContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void currentVersionTokenIsAcceptedByActualFilter() throws ServletException, IOException {
        User user = userWithVersion(1L);
        JwtService jwtService = new JwtService(SECRET, 60_000);
        CustomUserDetailsService detailsService = mock(CustomUserDetailsService.class);
        when(detailsService.loadUserByUsername(user.getNic())).thenReturn(new UserPrincipal(user));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, detailsService);

        invoke(filter, jwtService.generateToken(user.getNic(), "CUSTOMER", 1L));

        assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
    }

    @Test
    void olderVersionTokenIsRejectedByActualFilter() throws ServletException, IOException {
        User user = userWithVersion(1L);
        JwtService jwtService = new JwtService(SECRET, 60_000);
        CustomUserDetailsService detailsService = mock(CustomUserDetailsService.class);
        when(detailsService.loadUserByUsername(user.getNic())).thenReturn(new UserPrincipal(user));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, detailsService);

        invoke(filter, jwtService.generateToken(user.getNic(), "CUSTOMER", 0L));

        assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }

    @Test
    void legacyTokenWithoutClaimIsAcceptedOnlyForVersionZero() throws ServletException, IOException {
        User user = userWithVersion(0L);
        JwtService jwtService = new JwtService(SECRET, 60_000);
        CustomUserDetailsService detailsService = mock(CustomUserDetailsService.class);
        when(detailsService.loadUserByUsername(user.getNic())).thenReturn(new UserPrincipal(user));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, detailsService);

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(user.getNic())
                .claim("role", "CUSTOMER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
        invoke(filter, token);

        assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
    }

    private void invoke(JwtAuthenticationFilter filter, String token) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private User userWithVersion(long tokenVersion) {
        return User.builder()
                .id(java.util.UUID.randomUUID())
                .nic("200012345678")
                .email("auth@example.com")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .tokenVersion(tokenVersion)
                .build();
    }
}
