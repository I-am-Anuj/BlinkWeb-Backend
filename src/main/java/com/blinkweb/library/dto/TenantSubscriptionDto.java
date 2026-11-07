package com.blinkweb.library.dto;

import java.time.Instant;

public class TenantSubscriptionDto {

    private String tenantId;
    private String status; // ACTIVE, TRIALING, PAST_DUE, CANCELED, SUSPENDED
    private String planName;
    private Instant expiresAt;

    public TenantSubscriptionDto() {
    }

    public TenantSubscriptionDto(String tenantId, String status, String planName, Instant expiresAt) {
        this.tenantId = tenantId;
        this.status = status;
        this.planName = planName;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status) || "TRIALING".equalsIgnoreCase(status);
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
