package com.blinkweb.library.exception;

public class TenantSubscriptionInactiveException extends RuntimeException {

    private final String tenantId;
    private final String subscriptionStatus;

    public TenantSubscriptionInactiveException(String tenantId, String subscriptionStatus) {
        super(String.format("Subscription for tenant '%s' is inactive with status: %s. Access denied.", tenantId, subscriptionStatus));
        this.tenantId = tenantId;
        this.subscriptionStatus = subscriptionStatus;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }
}
