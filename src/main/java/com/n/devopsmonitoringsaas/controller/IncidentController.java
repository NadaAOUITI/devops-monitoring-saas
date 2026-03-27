package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.Incident;
import com.n.devopsmonitoringsaas.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants/{tenantId}")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping("/incidents")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN','ROLE_MEMBER')")
    public List<Incident> listIncidents(@PathVariable Long tenantId) {
        return incidentService.findByTenantId(tenantId);
    }

    @GetMapping("/incidents/{incidentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN','ROLE_MEMBER')")
    public Incident getIncident(@PathVariable Long tenantId, @PathVariable Long incidentId) {
        return incidentService.findByIdAndTenantId(incidentId, tenantId);
    }

    @GetMapping("/services/{serviceId}/incidents")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN','ROLE_MEMBER')")
    public List<Incident> listServiceIncidents(@PathVariable Long tenantId, @PathVariable Long serviceId) {
        return incidentService.findByServiceIdAndTenantId(serviceId, tenantId);
    }
}
