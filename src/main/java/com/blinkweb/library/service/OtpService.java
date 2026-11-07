package com.blinkweb.library.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing 6-digit One-Time Passwords (OTP) with in-memory caching and 5-minute TTL.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final long OTP_TTL_MINUTES = 5;

    public record OtpEntry(String code, Instant expiresAt) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a cryptographically random 6-digit OTP code, stores it with 5-minute TTL,
     * and returns the generated code.
     */
    public String generateOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        String normalizedEmail = email.trim().toLowerCase();
        int number = secureRandom.nextInt(1_000_000);
        String otp = String.format("%06d", number);

        Instant expiresAt = Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES);
        otpStorage.put(normalizedEmail, new OtpEntry(otp, expiresAt));

        log.info("🔐 Generated 6-digit OTP for [{}] (valid for {} minutes)", normalizedEmail, OTP_TTL_MINUTES);
        return otp;
    }

    /**
     * Verifies the provided OTP code against the stored value.
     * If valid, immediately removes the OTP (single-use security guarantee) and returns true.
     */
    public boolean validateOtp(String email, String otp) {
        if (email == null || otp == null) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedOtp = otp.trim();

        OtpEntry entry = otpStorage.get(normalizedEmail);
        if (entry == null) {
            log.warn("OTP validation failed: No active OTP found for [{}]", normalizedEmail);
            return false;
        }

        if (entry.isExpired()) {
            log.warn("OTP validation failed: OTP for [{}] has expired", normalizedEmail);
            otpStorage.remove(normalizedEmail);
            return false;
        }

        if (entry.code().equals(normalizedOtp)) {
            log.info("✅ OTP validation successful for [{}]", normalizedEmail);
            otpStorage.remove(normalizedEmail); // Invalidate immediately upon successful verification
            return true;
        }

        log.warn("OTP validation failed: Incorrect code supplied for [{}]", normalizedEmail);
        return false;
    }

    /**
     * Manually seeds an OTP (useful for automated testing).
     */
    public void seedOtp(String email, String otp, long ttlMinutes) {
        otpStorage.put(email.trim().toLowerCase(), new OtpEntry(otp, Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES)));
    }

    /**
     * Clears any active OTP for an email.
     */
    public void clearOtp(String email) {
        if (email != null) {
            otpStorage.remove(email.trim().toLowerCase());
        }
    }
}
