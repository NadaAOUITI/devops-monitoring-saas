package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.repository.PlanRepository;
import com.n.devopsmonitoringsaas.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;
    private final TenantService tenantService;

    @GetMapping("/plans")
    public List<Plan> listPlans() {
        return planRepository.findAll();
    }

    @PostMapping("/tenants/{tenantId}/plan")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public Tenant subscribeToPlan(@PathVariable Long tenantId, @Valid @RequestBody PlanSubscriptionRequest request) {
        return tenantService.subscribeToPlan(tenantId, request.planId());
    }

    @PutMapping("/tenants/{tenantId}/plan")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public Tenant changePlan(@PathVariable Long tenantId, @Valid @RequestBody PlanSubscriptionRequest request) {
        return tenantService.subscribeToPlan(tenantId, request.planId());
    }

    public record PlanSubscriptionRequest(@NotNull Long planId) {}
}
