package com.backend.water_management_system.internal_chat.service;

import com.backend.water_management_system.internal_chat.dto.CreateConversationRequest;
import com.backend.water_management_system.internal_chat.exceptions.InternalChatValidationException;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
/**
 * Verifies Phase 1 conversation creation, validation, and staff filtering
 * against the application context.
 */
class InternalChatServiceTest {

    @Autowired
    private InternalChatService internalChatService;

    @Autowired
    private UserRepository userRepository;

    private User currentUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        // Unique identities keep this test isolated from the application's seeded
        // relational data.
        String testRunId = UUID.randomUUID().toString();

        currentUser = userRepository.save(User.builder()
                .nic("NIC-100-" + testRunId)
                .fullName("Current Admin")
                .email("current-" + testRunId + "@example.com")
                .phoneNumber("111")
                .passwordHash("pw")
                .role(Role.SYSTEM_ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        targetUser = userRepository.save(User.builder()
                .nic("NIC-200-" + testRunId)
                .fullName("Target Admin")
                .email("target-" + testRunId + "@example.com")
                .phoneNumber("222")
                .passwordHash("pw")
                .role(Role.PAYMENT_HANDLER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    /**
     * Confirms that creating the same direct conversation twice reuses one
     * conversation.
     */
    void shouldCreateConversationAndReuseExistingConversation() {
        CreateConversationRequest request = new CreateConversationRequest(targetUser.getId());

        var first = internalChatService.createConversation(currentUser.getId(), request);
        var second = internalChatService.createConversation(currentUser.getId(), request);

        assertThat(first.id()).isEqualTo(second.id());
    }

    @Test
    /**
     * Confirms that the service rejects attempts to start a conversation with
     * oneself.
     */
    void shouldRejectSelfConversation() {
        CreateConversationRequest request = new CreateConversationRequest(currentUser.getId());

        assertThatThrownBy(() -> internalChatService.createConversation(currentUser.getId(), request))
                .isInstanceOf(InternalChatValidationException.class);
    }

    @Test
    /**
     * Confirms that customers and the current user are excluded from staff search
     * results.
     */
    void shouldReturnEligibleUsersOnly() {
        String testRunId = UUID.randomUUID().toString();
        User customer = userRepository.save(User.builder()
                .nic("NIC-300-" + testRunId)
                .fullName("Customer User")
                .email("customer-" + testRunId + "@example.com")
                .phoneNumber("333")
                .passwordHash("pw")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        var users = internalChatService.searchEligibleUsers(currentUser.getId(), null, "Admin");

        assertThat(users).extracting("id")
                .doesNotContain(currentUser.getId(), customer.getId());
    }
}
