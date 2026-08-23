package com.backend.water_management_system.activity_audit.service;

import com.backend.water_management_system.activity_audit.model.AuditActorSnapshot;
import com.backend.water_management_system.security.UserPrincipal;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditActorResolverTest {

    private final AuditActorResolver resolver = new AuditActorResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void snapshotsAuthenticatedPrincipalAndDoesNotTrackLaterChanges() {
        UUID id = UUID.randomUUID();
        User user = user(id, "Original Name", "998877665V", Role.SYSTEM_ADMIN);
        authenticate(user);

        AuditActorSnapshot snapshot = resolver.resolveAuthenticatedWebActor();
        user.setFullName("Changed Name");
        user.setRole(Role.SUPER_ADMIN);

        assertThat(snapshot.userId()).isEqualTo(id);
        assertThat(snapshot.displayName()).isEqualTo("Original Name");
        assertThat(snapshot.role()).isEqualTo(Role.SYSTEM_ADMIN);
    }

    @Test
    void publicWebFlowUsesControlledUnauthenticatedSnapshot() {
        assertThat(resolver.resolvePublicWebActor())
                .isEqualTo(new AuditActorSnapshot(null, "Unauthenticated user", null));
    }

    @Test
    void authenticatedFlowDoesNotSilentlyDowngradeToPublicActor() {
        assertThatThrownBy(resolver::resolveAuthenticatedWebActor)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void systemFlowUsesControlledSystemSnapshot() {
        assertThat(resolver.resolveSystemActor())
                .isEqualTo(new AuditActorSnapshot(null, "System", null));
    }

    @Test
    void missingDisplayNameUsesUuidAndNeverNic() {
        UUID id = UUID.randomUUID();
        User user = user(id, " ", "998877665V", Role.CUSTOMER);
        authenticate(user);

        AuditActorSnapshot snapshot = resolver.resolveAuthenticatedWebActor();

        assertThat(snapshot.displayName()).isEqualTo("User " + id.toString().substring(0, 8));
        assertThat(snapshot.displayName()).doesNotContain(user.getNic());
    }

    private void authenticate(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private User user(UUID id, String fullName, String nic, Role role) {
        return User.builder()
                .id(id)
                .fullName(fullName)
                .nic(nic)
                .email("not-audited@example.test")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
