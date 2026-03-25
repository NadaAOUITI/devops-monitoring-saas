package com.n.devopsmonitoringsaas.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("tenantSecurity")
public class TenantSecurity {

    public boolean sameTenant(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Object details = auth.getDetails();
        if (!(details instanceof Long jwtTenantId)) {
            return false;
        }
        return jwtTenantId.equals(tenantId);
    }
}
