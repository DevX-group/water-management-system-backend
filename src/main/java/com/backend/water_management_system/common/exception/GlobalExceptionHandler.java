package com.backend.water_management_system.common.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.backend.water_management_system.activity_audit.exception.ActivityAuditLogNotFoundException;
import com.backend.water_management_system.customer.exceptions.CustomerNotFoundException;
import com.backend.water_management_system.messaging.exceptions.MessagingNotFoundException;
import com.backend.water_management_system.messaging.exceptions.MessagingValidationException;
import com.backend.water_management_system.payments.exceptions.CloudinaryUploadException;
import com.backend.water_management_system.payments.exceptions.InvalidPaymentException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ActivityAuditLogNotFoundException.class)
    public ResponseEntity<ApiError> handleActivityAuditNotFound(ActivityAuditLogNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("Activity audit log not found.", "ACTIVITY_AUDIT_NOT_FOUND", 404));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Invalid request parameter.", "BAD_REQUEST", 400));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiError("Request method is not supported.", "METHOD_NOT_ALLOWED", 405));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiError(ex.getMessage(), "FORBIDDEN", 403));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleCustomerNotFound(CustomerNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(ex.getMessage(), "CUSTOMER_NOT_FOUND", 404));
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPayment(InvalidPaymentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(CloudinaryUploadException.class)
    public ResponseEntity<ApiError> handleCloudinaryUpload(CloudinaryUploadException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError(ex.getMessage(), "IMAGE_UPLOAD_FAILED", 502));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError("Image is too large. Maximum size is 10 MB.", "FILE_TOO_LARGE", 413));
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> handleMultipartFailure(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Please upload an image using the 'file' field.", "INVALID_FILE_UPLOAD", 400));
    }

    @ExceptionHandler(MessagingNotFoundException.class)
    public ResponseEntity<ApiError> handleMessagingNotFound(MessagingNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(ex.getMessage(), "MESSAGE_NOT_FOUND", 404));
    }

    @ExceptionHandler(MessagingValidationException.class)
    public ResponseEntity<ApiError> handleMessagingValidation(MessagingValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), "VALIDATION_ERROR", 400));
    }

        @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
        public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        String message = ex instanceof MethodArgumentNotValidException validationException
            ? validationException.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(error -> error != null && !error.isBlank())
                .collect(Collectors.joining(", "))
            : ex.getMessage();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ApiError(
                message == null || message.isBlank() ? "Invalid request." : message,
                "BAD_REQUEST",
                400
            ));
        }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        "Unexpected error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                        "INTERNAL_ERROR",
                        500
                ));
    }
}
