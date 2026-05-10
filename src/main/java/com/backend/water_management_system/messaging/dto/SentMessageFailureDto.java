package com.backend.water_management_system.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class SentMessageFailureDto {

    private String subscriptionNumber;
    private String customerName;
    private String phoneNumber;
    private String email;

    private boolean smsFailed;
    private boolean emailFailed;

}
