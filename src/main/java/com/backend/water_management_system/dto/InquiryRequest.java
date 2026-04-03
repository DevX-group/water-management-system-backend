package com.backend.water_management_system.dto;

import lombok.Data;

@Data
public class InquiryRequest {
    private String name;
    private String email;
    private String category;
    private String message;
}