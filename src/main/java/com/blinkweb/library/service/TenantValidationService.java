package com.blinkweb.library.service;

import com.blinkweb.library.dto.TenantSubscriptionDto;
import com.blinkweb.library.exception.TenantNotFoundException;
import com.blinkweb.library.exception.TenantSubscriptionInactiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for validating tenant existence and subscription status.
 *
 * Implements a 2-tier validation strategy:
 * 1. High-speed local cache with short TTL (e.g., 60 seconds) to avoid network latency on every HTTP request.
 * 2. Remote check against Blinkweb Core Backend (or local synced tenants table).
 */
@Service
public class TenantValidationService {

    private static final Logger log = LoggerFactory.getLogger(TenantValidationService.class);

    @Value("${blinkweb.core.backend-url:https://core.blinkweb.io}")
    private String coreBackendUrl;

    @Value("${blinkweb.core.cache-ttl-seconds:60}")
    private long cacheTtlSeconds;

    // Cache entry wrapper with expiration timestamp
    private record CachedSubscription(TenantSubscriptionDto subscription, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, CachedSubscription> cache = new ConcurrentHashMap<>();

    public TenantValidationService() {
        // Pre-seed default sandbox/demo tenants for development and automated testing
        seedLocalTenant("acme-library", "ACTIVE", "ENTERPRISE");
        seedLocalTenant("city-books", "ACTIVE", "PRO");
        seedLocalTenant("expired-tenant", "PAST_DUE", "BASIC");
        seedLocalTenant("suspended-tenant", "SUSPENDED", "STARTER");
    }

    /**
     * Validates whether a tenant exists and has an active subscription.
     * Throws TenantSubscriptionInactiveException (HTTP 402) or TenantNotFoundException (HTTP 404).
     */
    public TenantSubscriptionDto validateTenantSubscription(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or blank");
        }

        String normalizedTenantId = tenantId.trim().toLowerCase();

        // 1. Check local in-memory cache
        CachedSubscription cached = cache.get(normalizedTenantId);
        if (cached != null && !cached.isExpired()) {
            return checkSubscriptionStatus(cached.subscription());
        }

        // 2. Fetch from Blinkweb Core Backend (or synced database)
        TenantSubscriptionDto subscription = fetchFromCoreBackend(normalizedTenantId);

        // 3. Cache the verified subscription
        cache.put(normalizedTenantId, new CachedSubscription(
                subscription,
                Instant.now().plus(cacheTtlSeconds, ChronoUnit.SECONDS)
        ));

        // 4. Enforce active status
        return checkSubscriptionStatus(subscription);
    }

    /**
     * Checks if subscription status allows access (ACTIVE or TRIALING).
     * Rejects PAST_DUE, SUSPENDED, CANCELED, or EXPIRED.
     */
    private TenantSubscriptionDto checkSubscriptionStatus(TenantSubscriptionDto sub) {
        if (!sub.isActive()) {
            log.warn("Access denied for tenant [{}]: Subscription status is [{}]", sub.getTenantId(), sub.getStatus());
            throw new TenantSubscriptionInactiveException(sub.getTenantId(), sub.getStatus());
        }
        return sub;
    }

    /**
     * Simulates or calls Blinkweb Core Backend internal endpoint.
     * In a live microservice deployment, this invokes:
     * GET {coreBackendUrl}/api/v1/internal/tenants/{tenantId}/subscription
     */
    private TenantSubscriptionDto fetchFromCoreBackend(String tenantId) {
        log.info("Fetching subscription status from Blinkweb Core Backend for tenant [{}] at {}", tenantId, coreBackendUrl);

        // Here we simulate the remote API lookup. If not present in known map, throw 404
        CachedSubscription existing = cache.get(tenantId);
        if (existing != null) {
            return existing.subscription();
        }

        // Fallback for demo or unrecognized tenants
        throw new TenantNotFoundException(tenantId);
    }

    /**
     * Helper to register or mock tenants in tests / dev mode.
     */
    public void seedLocalTenant(String tenantId, String status, String plan) {
        TenantSubscriptionDto dto = new TenantSubscriptionDto(
                tenantId.toLowerCase(),
                status.toUpperCase(),
                plan,
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        cache.put(tenantId.toLowerCase(), new CachedSubscription(dto, Instant.now().plus(1, ChronoUnit.HOURS)));
    }

    public void evictCache(String tenantId) {
        cache.remove(tenantId.toLowerCase());
    }
}
