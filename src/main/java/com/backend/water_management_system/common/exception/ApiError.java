package com.backend.water_management_system.common.exception;

import lombok.Getter;

@Getter
public class ApiError {

    private final String message;
    private final String code;
    private final int status;
    private final long timestamp;

    public ApiError(String message, String code, int status) {
        this.message = message;
        this.code = code;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }
}
