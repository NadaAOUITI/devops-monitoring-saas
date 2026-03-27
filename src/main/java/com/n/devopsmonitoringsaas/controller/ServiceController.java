package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.controller.dto.ServiceCreateRequest;
import com.n.devopsmonitoringsaas.entity.Service;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import com.n.devopsmonitoringsaas.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants/{tenantId}/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;
    private final TenantRepository tenantRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN')")
    public Service registerService(
            @PathVariable Long tenantId,
            @Valid @RequestBody ServiceCreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        Service service = Service.builder()
                .name(request.name())
                .url(request.url())
                .pingIntervalSeconds(request.pingIntervalSeconds())
                .tenant(tenant)
                .build();

        return serviceService.registerService(service);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN','ROLE_MEMBER')")
    public List<Service> listServices(@PathVariable Long tenantId) {
        return serviceService.findByTenantId(tenantId);
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN')")
    public Service updateService(
            @PathVariable Long tenantId,
            @PathVariable Long serviceId,
            @Valid @RequestBody ServiceCreateRequest request) {
        return serviceService.updateService(
                tenantId, serviceId, request.name(), request.url(), request.pingIntervalSeconds());
    }

    @DeleteMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN')")
    public void deleteService(@PathVariable Long tenantId, @PathVariable Long serviceId) {
        serviceService.deleteService(tenantId, serviceId);
    }
}
