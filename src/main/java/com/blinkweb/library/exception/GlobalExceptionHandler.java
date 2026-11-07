package com.blinkweb.library.exception;

import com.blinkweb.library.context.TenantContext;
import com.blinkweb.library.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception translator mapping multi-tenancy and domain errors
 * to appropriate HTTP status codes and structured payloads.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TenantResolutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantResolution(TenantResolutionException ex) {
        log.warn("Tenant Resolution Failed: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                "MISSING_OR_INVALID_TENANT_ID",
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maps inactive subscription to HTTP 402 (Payment Required).
     */
    @ExceptionHandler(TenantSubscriptionInactiveException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleSubscriptionInactive(TenantSubscriptionInactiveException ex) {
        log.warn("Subscription Inactive: tenant [{}] status [{}]", ex.getTenantId(), ex.getSubscriptionStatus());
        Map<String, String> details = new HashMap<>();
        details.put("tenantId", ex.getTenantId());
        details.put("subscriptionStatus", ex.getSubscriptionStatus());
        details.put("resolutionUrl", "https://app.blinkweb.io/billing");

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                ex.getMessage(),
                "SUBSCRIPTION_INACTIVE",
                ex.getTenantId()
        );
        response.setData(details);

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantNotFound(TenantNotFoundException ex) {
        log.warn("Tenant Not Found: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                "TENANT_NOT_FOUND",
                ex.getTenantId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                "RESOURCE_NOT_FOUND",
                TenantContext.getTenantId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityViolation(SecurityException ex) {
        log.error("Tenant Security Violation: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                "CROSS_TENANT_ACCESS_DENIED",
                TenantContext.getTenantId()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "Input validation failed",
                "VALIDATION_ERROR",
                TenantContext.getTenantId()
        );
        response.setData(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                "BAD_REQUEST",
                TenantContext.getTenantId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);
        ApiResponse<Void> response = ApiResponse.error(
                "An internal error occurred. Please try again later.",
                "INTERNAL_SERVER_ERROR",
                TenantContext.getTenantId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
