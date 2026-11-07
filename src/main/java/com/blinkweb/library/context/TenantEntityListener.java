package com.blinkweb.library.context;

import com.blinkweb.library.entity.BaseTenantEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA EntityListener that automatically enforces row-level tenant integrity.
 * - On @PrePersist: Populates the tenant_id field from TenantContext if not explicitly set.
 * - On @PreUpdate & @PreRemove: Verifies the entity belongs to the current tenant to prevent
 *   accidental cross-tenant modifications.
 */
public class TenantEntityListener {

    private static final Logger log = LoggerFactory.getLogger(TenantEntityListener.class);

    @PrePersist
    public void onPrePersist(BaseTenantEntity entity) {
        String currentTenant = TenantContext.getTenantId();

        if (currentTenant == null || currentTenant.isBlank()) {
            throw new IllegalStateException("Security Violation: Cannot persist tenant entity without an active TenantContext");
        }

        if (entity.getTenantId() == null) {
            log.debug("Auto-assigning tenant_id [{}] to new entity of type {}", currentTenant, entity.getClass().getSimpleName());
            entity.setTenantId(currentTenant);
        } else if (!entity.getTenantId().equalsIgnoreCase(currentTenant)) {
            throw new SecurityException(String.format(
                    "Tenant Mismatch: Entity tenant [%s] does not match current security context tenant [%s]",
                    entity.getTenantId(), currentTenant));
        }
    }

    @PreUpdate
    public void onPreUpdate(BaseTenantEntity entity) {
        verifyTenantAccess(entity, "UPDATE");
    }

    @PreRemove
    public void onPreRemove(BaseTenantEntity entity) {
        verifyTenantAccess(entity, "DELETE");
    }

    private void verifyTenantAccess(BaseTenantEntity entity, String operation) {
        String currentTenant = TenantContext.getTenantId();
        if (currentTenant != null && !currentTenant.equalsIgnoreCase(entity.getTenantId())) {
            throw new SecurityException(String.format(
                    "Security Violation: Attempted %s on entity with tenant_id [%s] under TenantContext [%s]",
                    operation, entity.getTenantId(), currentTenant));
        }
    }
}
