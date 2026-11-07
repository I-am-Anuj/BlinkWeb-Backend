package com.blinkweb.library.context;

/**
 * Thread-local context holder for the current request's Tenant ID.
 * This guarantees tenant context availability throughout the entire execution
 * thread (Controllers, Services, Repositories, and JPA Entity Listeners).
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Prevent instantiation
    }

    /**
     * Set the current tenant ID for this thread.
     *
     * @param tenantId the resolved tenant identifier (e.g. subdomain prefix)
     */
    public static void setTenantId(String tenantId) {
        if (tenantId != null) {
            CURRENT_TENANT.set(tenantId.trim().toLowerCase());
        } else {
            CURRENT_TENANT.remove();
        }
    }

    /**
     * Get the current tenant ID for this thread.
     *
     * @return current tenant ID or null if unassigned
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clear the tenant context.
     * CRITICAL: Must be invoked in afterCompletion / finally block
     * to avoid thread-pool pollution in servlet containers (e.g. Tomcat).
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
