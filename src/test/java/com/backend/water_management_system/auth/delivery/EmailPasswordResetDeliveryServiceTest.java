package com.backend.water_management_system.auth.delivery;

import com.backend.water_management_system.auth.entity.DeliveryChannel;
import com.backend.water_management_system.auth.entity.PasswordResetPurpose;
import com.backend.water_management_system.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailPasswordResetDeliveryServiceTest {

    @Test
    void eligibleContextIsRenderedAsPasswordRecoveryEmail() {
        MailSender mailSender = mock(MailSender.class);
        com.backend.water_management_system.user.repository.UserRepository userRepository = mock(com.backend.water_management_system.user.repository.UserRepository.class);
        User user = User.builder().id(UUID.randomUUID()).email("recovery@example.com").build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        EmailPasswordResetDeliveryService delivery = new EmailPasswordResetDeliveryService(mailSender, userRepository);
        PasswordResetDeliveryContext context = new PasswordResetDeliveryContext(
                user.getId(), DeliveryChannel.EMAIL, "123456", Instant.now().plusSeconds(300),
                PasswordResetPurpose.PASSWORD_RESET);

        delivery.deliver(context);

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("recovery@example.com");
        assertThat(captor.getValue().getText()).contains("123456", "password recovery", "five minutes")
                .doesNotContain("passwordHash");
    }

    @Test
    void unsupportedChannelIsRejectedByCapabilityCheck() {
        EmailPasswordResetDeliveryService delivery = new EmailPasswordResetDeliveryService(
                mock(MailSender.class), mock(com.backend.water_management_system.user.repository.UserRepository.class));

        assertThat(delivery.supports(DeliveryChannel.EMAIL)).isTrue();
        assertThat(delivery.supports(DeliveryChannel.SMS)).isFalse();
    }
}
