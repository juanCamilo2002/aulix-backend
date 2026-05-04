package com.aulix.aulix_backend.tenant;

public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT =
            new InheritableThreadLocal<>();

    private TenantContext() {}

    public static void setTenant(String tenantSlug) {
        CURRENT_TENANT.set(tenantSlug);
    }

    public static String getTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}

