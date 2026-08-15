package com.backend.water_management_system.payments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "payhere")
public class PayHereConfig {
    private String merchantId;
    private String merchantSecret;
    private String returnUrl;
    private String cancelUrl;
    private String notifyUrl;
}
