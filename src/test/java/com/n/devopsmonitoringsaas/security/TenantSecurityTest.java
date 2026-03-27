package com.n.devopsmonitoringsaas.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantSecurityTest {

    private final TenantSecurity tenantSecurity = new TenantSecurity();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sameTenant returns true when JWT tenant matches path tenant")
    void sameTenant_returnsTrue_whenMatch() {
        var auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        auth.setDetails(10L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(tenantSecurity.sameTenant(10L)).isTrue();
    }

    @Test
    @DisplayName("sameTenant returns false when JWT tenant does not match path tenant")
    void sameTenant_returnsFalse_whenMismatch() {
        var auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        auth.setDetails(10L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(tenantSecurity.sameTenant(99L)).isFalse();
    }

    @Test
    @DisplayName("sameTenant returns false when not authenticated")
    void sameTenant_returnsFalse_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThat(tenantSecurity.sameTenant(10L)).isFalse();
    }

    @Test
    @DisplayName("sameTenant returns false when tenantId path is null")
    void sameTenant_returnsFalse_whenTenantIdNull() {
        var auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
        auth.setDetails(10L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(tenantSecurity.sameTenant(null)).isFalse();
    }

    @Test
    @DisplayName("sameTenant returns false when auth details are not Long tenant id")
    void sameTenant_returnsFalse_whenDetailsNotLong() {
        var auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        auth.setDetails("not-a-long");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(tenantSecurity.sameTenant(10L)).isFalse();
    }

    @Test
    @DisplayName("sameTenant returns false when authentication is null")
    void sameTenant_returnsFalse_whenAuthenticationNull() {
        SecurityContextHolder.clearContext();
        assertThat(tenantSecurity.sameTenant(10L)).isFalse();
    }

    @Test
    @DisplayName("sameTenant returns false when authentication is not authenticated")
    void sameTenant_returnsFalse_whenNotFullyAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        auth.setAuthenticated(false);
        auth.setDetails(10L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(tenantSecurity.sameTenant(10L)).isFalse();
    }
}
