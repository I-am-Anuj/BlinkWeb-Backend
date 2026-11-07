package com.blinkweb.library.exception;

public class TenantNotFoundException extends RuntimeException {

    private final String tenantId;

    public TenantNotFoundException(String tenantId) {
        super(String.format("Tenant '%s' was not found in Blinkweb platform directory.", tenantId));
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }
}
