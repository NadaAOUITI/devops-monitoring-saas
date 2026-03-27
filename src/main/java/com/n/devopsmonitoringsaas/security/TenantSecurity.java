package com.n.devopsmonitoringsaas.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("tenantSecurity")
public class TenantSecurity {

    /** Used by tests and optional SpEL; tenant path matching is enforced by {@link TenantPathSecurityFilter}. */
    public boolean sameTenant(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Object details = auth.getDetails();
        if (!(details instanceof Number jwtTenantNum)) {
            return false;
        }
        return jwtTenantNum.longValue() == tenantId.longValue();
    }
}
