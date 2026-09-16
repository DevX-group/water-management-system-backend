package com.backend.water_management_system.common.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandlerForDebug {

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<String> handleException(Exception e) {
        e.printStackTrace();
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get("debug-error.log"), 
                e.getClass().getName() + ": " + e.getMessage() + "\n" + 
                java.util.Arrays.stream(e.getStackTrace())
                    .map(StackTraceElement::toString)
                    .reduce((a, b) -> a + "\n" + b).orElse("")
            );
        } catch(Exception ignored) {}
        
        return org.springframework.http.ResponseEntity.status(500).body(e.getMessage());
    }
}
