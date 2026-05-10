package com.backend.water_management_system.messaging.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class SMSGatewayResponseDTO {

    private String status;
    private String message;
    private Data data;

    @NoArgsConstructor
    @Getter
    @Setter
    public static class Data {

        private String to;
        private String from;
        private String message;
        private String status;
        private BigDecimal cost;
        private Integer smsCount;
    }
}
