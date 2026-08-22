package com.backend.water_management_system.auth.delivery;

import com.backend.water_management_system.auth.entity.DeliveryChannel;

public interface PasswordResetDeliveryService {

    boolean supports(DeliveryChannel deliveryChannel);

    void deliver(PasswordResetDeliveryContext context);
}