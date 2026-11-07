package com.blinkweb.library.interceptor;

import com.blinkweb.library.context.TenantContext;
import com.blinkweb.library.exception.TenantResolutionException;
import com.blinkweb.library.service.TenantValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URI;
import java.util.List;

/**
 * Spring HandlerInterceptor that extracts the tenant_id from the incoming request,
 * validates the tenant's subscription status, and binds it to the TenantContext.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantValidationService tenantValidationService;

    @Value("${blinkweb.multitenancy.root-domains:blinkweb.com,blinkweb.io,localhost}")
    private List<String> rootDomains;

    public TenantInterceptor(TenantValidationService tenantValidationService) {
        this.tenantValidationService = tenantValidationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip pre-flight CORS OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Exempt authentication endpoints from tenant resolution
        if (request.getRequestURI().startsWith("/api/auth/")) {
            return true;
        }

        // 1. Resolve tenant_id
        String tenantId = resolveTenantId(request);

        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantResolutionException(
                    "Tenant identification failed. Supply the 'X-Tenant-ID' header or access via a tenant subdomain (e.g. {tenant}.blinkweb.com)."
            );
        }

        // 2. Validate tenant & subscription active status (Throws 402/403 if inactive)
        tenantValidationService.validateTenantSubscription(tenantId);

        // 3. Bind to ThreadLocal TenantContext
        TenantContext.setTenantId(tenantId);
        log.debug("TenantContext bound to tenant_id: [{}] for path: [{}]", tenantId, request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // CRITICAL: Clean up ThreadLocal to prevent tenant pollution across pooled servlet threads
        String tenantId = TenantContext.getTenantId();
        TenantContext.clear();
        log.debug("TenantContext cleared for tenant_id: [{}]", tenantId);
    }

    /**
     * Resolves tenant ID using header fallback strategy:
     * 1. Check custom X-Tenant-ID header
     * 2. Inspect Origin header domain
     * 3. Inspect Referer header domain
     * 4. Inspect Host header domain
     */
    private String resolveTenantId(HttpServletRequest request) {
        // Strategy 1: Explicit header
        String headerTenant = request.getHeader(TENANT_HEADER);
        if (headerTenant != null && !headerTenant.isBlank()) {
            return headerTenant.trim().toLowerCase();
        }

        // Strategy 2: Origin header (e.g., https://my-library.blinkweb.com)
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            String tenantFromOrigin = extractSubdomainFromUrl(origin);
            if (tenantFromOrigin != null) {
                return tenantFromOrigin;
            }
        }

        // Strategy 3: Referer header
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            String tenantFromReferer = extractSubdomainFromUrl(referer);
            if (tenantFromReferer != null) {
                return tenantFromReferer;
            }
        }

        // Strategy 4: Host header (e.g., my-library.blinkweb.com:8080)
        String host = request.getHeader("Host");
        if (host != null && !host.isBlank()) {
            return extractSubdomainFromHost(host);
        }

        return null;
    }

    private String extractSubdomainFromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return extractSubdomainFromHost(host);
        } catch (Exception e) {
            log.trace("Failed to parse URL for tenant extraction: {}", url);
            return null;
        }
    }

    private String extractSubdomainFromHost(String hostWithPort) {
        if (hostWithPort == null || hostWithPort.isBlank()) {
            return null;
        }

        // Strip port if present
        String host = hostWithPort.contains(":") ? hostWithPort.split(":")[0] : hostWithPort;
        host = host.toLowerCase().trim();

        // Check against recognized root domains
        for (String rootDomain : rootDomains) {
            if (host.endsWith("." + rootDomain)) {
                String prefix = host.substring(0, host.length() - rootDomain.length() - 1);
                // Return immediate subdomain (or top-level tenant slug)
                if (!prefix.isBlank() && !prefix.contains("www")) {
                    return prefix;
                }
            }
        }

        // Special handling for localhost subdomains (e.g., acme.localhost)
        if (host.endsWith(".localhost")) {
            return host.substring(0, host.indexOf(".localhost"));
        }

        return null;
    }
}
