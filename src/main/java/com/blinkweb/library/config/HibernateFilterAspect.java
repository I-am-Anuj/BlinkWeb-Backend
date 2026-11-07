package com.blinkweb.library.config;

import com.blinkweb.library.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect that ensures the Hibernate "tenantFilter" is enabled for every
 * database interaction within Services and Repositories.
 *
 * This injects the tenant_id parameter into the Hibernate Session so that all
 * generated SQL queries seamlessly include: AND tenant_id = :tenantId
 */
@Aspect
@Component
public class HibernateFilterAspect {

    private static final Logger log = LoggerFactory.getLogger(HibernateFilterAspect.class);
    public static final String TENANT_FILTER_NAME = "tenantFilter";
    public static final String TENANT_PARAM_NAME = "tenantId";

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.blinkweb.library.service..*(..)) || execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableTenantFilter() {
        String tenantId = TenantContext.getTenantId();

        if (tenantId == null || tenantId.isBlank()) {
            log.trace("No active tenant in TenantContext. Skipping Hibernate tenant filter activation.");
            return;
        }

        try {
            Session session = entityManager.unwrap(Session.class);
            if (session != null && session.isOpen()) {
                Filter existingFilter = session.getEnabledFilter(TENANT_FILTER_NAME);
                if (existingFilter == null) {
                    log.debug("Activating Hibernate filter [{}] with tenantId [{}]", TENANT_FILTER_NAME, tenantId);
                    Filter filter = session.enableFilter(TENANT_FILTER_NAME);
                    filter.setParameter(TENANT_PARAM_NAME, tenantId);
                    filter.validate();
                }
            }
        } catch (Exception ex) {
            log.warn("Could not enable Hibernate tenant filter: {}", ex.getMessage());
        }
    }
}
