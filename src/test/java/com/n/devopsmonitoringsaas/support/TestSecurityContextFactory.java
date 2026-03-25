package com.n.devopsmonitoringsaas.support;

import com.n.devopsmonitoringsaas.entity.UserRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Sets {@link SecurityContextHolder} the same way {@code JwtAuthFilter} does for tests
 * that invoke secured layers (e.g. {@code @PreAuthorize} on controllers via MockMvc).
 */
public final class TestSecurityContextFactory {

    private TestSecurityContextFactory() {}

    public static void authenticate(Long userId, Long tenantId, UserRole role) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        auth.setDetails(tenantId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
