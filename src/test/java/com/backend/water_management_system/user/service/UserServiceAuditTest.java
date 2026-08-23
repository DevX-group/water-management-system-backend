package com.backend.water_management_system.user.service;

import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
import com.backend.water_management_system.auth.service.AuthService;
import com.backend.water_management_system.user.dto.UserCreateRequest;
import com.backend.water_management_system.user.dto.UserUpdateRequest;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAuditTest {

    @Mock UserRepository userRepository;
    @Mock AuthService authService;
    @Mock ActivityAuditService auditService;
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, authService, auditService);
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) user.setId(UUID.randomUUID());
            return user;
        });
    }

    @Test
    void administrativeCreationRecordsExactlyOneAuthenticatedSafeEntry() {
        service.createUser(createRequest(Role.CUSTOMER), Role.SUPER_ADMIN);

        ArgumentCaptor<Map<String, String>> details = mapCaptor();
        verify(auditService).recordAuthenticatedWeb(eq(AuditAction.USER_CREATED),
                eq(AuditEntityType.USER), any(UUID.class), details.capture());
        verify(auditService, never()).recordPublicWeb(any(), any(), any(), any());
        assertThat(details.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "role", "CUSTOMER", "status", "PENDING_ACTIVATION"));
        assertSafe(details.getValue());
    }

    @Test
    void trustedPublicCreationRecordsExactlyOnePublicEntry() {
        service.createPublicCustomerUser(createRequest(Role.CUSTOMER), Role.SYSTEM_ADMIN);

        verify(auditService).recordPublicWeb(eq(AuditAction.USER_CREATED),
                eq(AuditEntityType.USER), any(UUID.class), any());
        verify(auditService, never()).recordAuthenticatedWeb(any(), any(), any(), any());
    }

    @Test
    void profileAndRoleChangesAreSeparateAndContainOnlyMarkersAndEnums() {
        User user = user(Role.SYSTEM_ADMIN, UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        service.updateAdmin(user.getId(),
                new UserUpdateRequest("new-nic", "New Name", "new@example.test",
                        Role.CUSTOMER_HANDLER, "0777777777"), Role.SUPER_ADMIN);

        ArgumentCaptor<AuditAction> actions = ArgumentCaptor.forClass(AuditAction.class);
        ArgumentCaptor<Map<String, String>> details = mapCaptor();
        verify(auditService, times(2)).recordAuthenticatedWeb(actions.capture(),
                eq(AuditEntityType.USER), eq(user.getId()), details.capture());
        assertThat(actions.getAllValues()).containsExactly(
                AuditAction.USER_PROFILE_UPDATED, AuditAction.USER_ROLE_CHANGED);
        assertThat(details.getAllValues().get(0)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "fullName", "changed", "nic", "changed",
                "email", "changed", "phoneNumber", "changed"));
        assertThat(details.getAllValues().get(1)).containsEntry(
                "role", "SYSTEM_ADMIN -> CUSTOMER_HANDLER");
        details.getAllValues().forEach(UserServiceAuditTest::assertSafe);
    }

    @Test
    void unchangedProfileRoleAndStatusCreateNoEntries() {
        User user = user(Role.SYSTEM_ADMIN, UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        service.updateAdmin(user.getId(), new UserUpdateRequest(
                user.getNic(), user.getFullName(), user.getEmail(), user.getRole(), user.getPhoneNumber()),
                Role.SUPER_ADMIN);
        service.updateUserStatus(user.getId(), UserStatus.ACTIVE, Role.SUPER_ADMIN);

        verifyNoInteractions(auditService);
    }

    @Test
    void statusChangeRecordsEnumTransition() {
        User user = user(Role.CUSTOMER, UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        service.updateUserStatus(user.getId(), UserStatus.SUSPENDED, Role.SYSTEM_ADMIN);

        verify(auditService).recordAuthenticatedWeb(AuditAction.USER_STATUS_CHANGED,
                AuditEntityType.USER, user.getId(), Map.of("status", "ACTIVE -> SUSPENDED"));
    }

    private static UserCreateRequest createRequest(Role role) {
        return new UserCreateRequest("sensitive-nic", "Sensitive Name",
                "sensitive@example.test", role, "0712345678");
    }

    private static User user(Role role, UserStatus status) {
        return User.builder().id(UUID.randomUUID()).nic("old-nic").fullName("Old Name")
                .email("old@example.test").phoneNumber("0700000000")
                .role(role).status(status).build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Map<String, String>> mapCaptor() {
        return ArgumentCaptor.forClass((Class) Map.class);
    }

    private static void assertSafe(Map<String, String> details) {
        String serialized = details.toString();
        assertThat(serialized).doesNotContain("sensitive-nic", "Sensitive Name",
                "sensitive@example.test", "0712345678", "old-nic", "Old Name",
                "old@example.test", "0700000000", "new-nic", "New Name",
                "new@example.test", "0777777777");
    }
}
