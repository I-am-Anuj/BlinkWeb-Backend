package com.blinkweb.library.controller;

import com.blinkweb.library.dto.ApiResponse;
import com.blinkweb.library.dto.AuthResponse;
import com.blinkweb.library.dto.AuthUserDto;
import com.blinkweb.library.dto.OtpSendRequest;
import com.blinkweb.library.dto.OtpVerifyRequest;
import com.blinkweb.library.service.EmailService;
import com.blinkweb.library.service.JwtService;
import com.blinkweb.library.service.OtpService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authentication Controller providing Email + OTP login and JWT token issuance.
 * Exempt from TenantInterceptor because authentication is platform-wide.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;

    public AuthController(OtpService otpService, EmailService emailService, JwtService jwtService) {
        this.otpService = otpService;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/auth/otp/send
     * Generates a 6-digit OTP code and dispatches it to the recipient's email.
     */
    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("Request received to send OTP to [{}]", email);

        String otp = otpService.generateOtp(email);
        boolean emailDelivered = emailService.sendOtpEmail(email, otp);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("expiresInSeconds", 300);

        if (emailDelivered) {
            response.put("message", "A 6-digit security code has been sent to " + email + ". Enter it below to verify.");
        } else {
            response.put("message", "Email not sent (unconfigured SMTP). DEV CODE: " + otp);
            response.put("testOtp", otp);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/otp/verify
     * Verifies the 6-digit OTP code, sets admin role for freesubscribe897@gmail.com,
     * and returns a signed JWT token with user details.
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        String email = request.email().trim().toLowerCase();
        String otp = request.otp().trim();

        log.info("Validating OTP for [{}]", email);

        boolean isValid = otpService.validateOtp(email, otp);
        if (!isValid) {
            log.warn("Invalid or expired OTP for [{}]", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid or expired security code. Please request a new one.", "INVALID_OTP", null));
        }

        // Determine user role and ID
        String role = "user";
        String userId = "usr_" + Math.abs(email.hashCode());

        if ("freesubscribe897@gmail.com".equalsIgnoreCase(email)) {
            role = "admin";
            userId = "usr_admin";
        }

        // Generate signed JWT token
        String token = jwtService.generateToken(email, role, userId);
        AuthUserDto userDto = new AuthUserDto(userId, email, role);

        log.info("Authentication successful for [{}] as role [{}]", email, role);

        return ResponseEntity.ok(new AuthResponse(true, token, userDto));
    }

    /**
     * GET /api/auth/me
     * Resolves the current user from the Authorization Bearer JWT header.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Missing or malformed Authorization header", "UNAUTHORIZED", null));
        }

        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid or expired token", "TOKEN_EXPIRED", null));
        }

        Claims claims = jwtService.extractClaims(token);
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        String userId = claims.get("userId", String.class);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", new AuthUserDto(userId, email, role)
        ));
    }

    /**
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully", null));
    }
}
