package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepository;

    @GetMapping("/{tenantId}")
    public Tenant getTenant(@PathVariable Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    }
}
