package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Service;
import com.n.devopsmonitoringsaas.entity.Tenant;

import java.util.List;
import com.n.devopsmonitoringsaas.exception.PlanLimitExceededException;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public Service registerService(Service service) {
        Tenant tenant = service.getTenant();
        if (tenant == null || tenant.getId() == null) {
            throw new IllegalArgumentException("Service must have a tenant with id");
        }

        tenant = tenantRepository.findById(tenant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        long count = serviceRepository.countByTenantId(tenant.getId());
        int maxServices = tenant.getPlan().getMaxServices();

        if (count >= maxServices) {
            throw new PlanLimitExceededException(
                    "Service limit reached. Plan allows max %d services, tenant has %d.".formatted(maxServices, count));
        }

        service.setTenant(tenant);
        return serviceRepository.save(service);
    }

    public List<Service> findByTenantId(Long tenantId) {
        return serviceRepository.findByTenantId(tenantId);
    }
}
