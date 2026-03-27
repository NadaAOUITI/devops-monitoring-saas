package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import com.n.devopsmonitoringsaas.service.TenantService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepository;
    private final TenantService tenantService;

    @GetMapping("/{tenantId}")
    @PreAuthorize("@tenantSecurity.sameTenant(#tenantId) and hasAnyRole('OWNER','ADMIN','MEMBER')")
    public Tenant getTenant(@PathVariable Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    }

    @PutMapping("/{tenantId}/webhook")
    @PreAuthorize("@tenantSecurity.sameTenant(#tenantId) and hasRole('OWNER')")
    public Tenant updateWebhook(
            @PathVariable Long tenantId,
            @RequestBody WebhookUpdateRequest request) {
        return tenantService.updateWebhookUrl(tenantId, request.webhookUrl());
    }

    public record WebhookUpdateRequest(
            @Size(max = 500) String webhookUrl
    ) {}
}
