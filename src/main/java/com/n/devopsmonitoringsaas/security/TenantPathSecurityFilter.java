package com.n.devopsmonitoringsaas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enforces that JWT tenant id (stored in {@link Authentication#getDetails()}) matches {@code /tenants/{tenantId}/...}
 * paths. Method-security SpEL parameter binding for {@code #tenantId} was unreliable across environments.
 */
public class TenantPathSecurityFilter extends OncePerRequestFilter {

    private static final Pattern TENANT_URI = Pattern.compile("^/tenants/(\\d+)(?:/.*)?$");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        Matcher m = TENANT_URI.matcher(uri);
        if (!m.matches()) {
            filterChain.doFilter(request, response);
            return;
        }
        long pathTenantId = Long.parseLong(m.group(1));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        Long jwtTenantId = extractJwtTenantId(auth);
        if (jwtTenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (jwtTenantId != pathTenantId) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static Long extractJwtTenantId(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
