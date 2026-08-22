package com.backend.water_management_system.auth.delivery;

import com.backend.water_management_system.auth.entity.DeliveryChannel;
import com.backend.water_management_system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailPasswordResetDeliveryService implements PasswordResetDeliveryService {

    private final MailSender mailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public boolean supports(DeliveryChannel deliveryChannel) {
        return deliveryChannel == DeliveryChannel.EMAIL;
    }

    @Override
    public void deliver(PasswordResetDeliveryContext context) {
        userRepository.findById(context.getUserId()).ifPresent(user -> {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }
            message.setTo(user.getEmail());
            message.setSubject("Password recovery code - Water Bill Management System");
            message.setText("Water Bill Management System\n\n"
                    + "Your password recovery verification code is: " + context.getRawOtp() + "\n\n"
                    + "This code expires in five minutes.\n"
                    + "If you did not request password recovery, please ignore this email.\n");
            mailSender.send(message);
        });
    }
}
