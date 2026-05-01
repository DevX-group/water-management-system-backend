package com.backend.water_management_system.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class SMSGatewayRequestDTO {

    private String recipient;
    private String sender_id;
    private String type;
    private String message;
}
