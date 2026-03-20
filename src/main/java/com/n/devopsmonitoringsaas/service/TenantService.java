package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.exception.PlanLimitExceededException;
import com.n.devopsmonitoringsaas.repository.PlanRepository;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final ServiceRepository serviceRepository;

    @Transactional
    public Tenant subscribeToPlan(Long tenantId, Long planId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        long serviceCount = serviceRepository.countByTenantId(tenantId);
        if (serviceCount > plan.getMaxServices()) {
            throw new PlanLimitExceededException(
                    "Tenant has %d services but plan '%s' allows max %d. Remove services first."
                            .formatted(serviceCount, plan.getName(), plan.getMaxServices()));
        }

        tenant.setPlan(plan);
        return tenantRepository.save(tenant);
    }
}
