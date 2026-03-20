package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.Incident;
import com.n.devopsmonitoringsaas.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants/{tenantId}")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping("/incidents")
    public List<Incident> listIncidents(@PathVariable Long tenantId) {
        return incidentService.findByTenantId(tenantId);
    }

    @GetMapping("/incidents/{incidentId}")
    public Incident getIncident(@PathVariable Long tenantId, @PathVariable Long incidentId) {
        return incidentService.findByIdAndTenantId(incidentId, tenantId);
    }

    @GetMapping("/services/{serviceId}/incidents")
    public List<Incident> listServiceIncidents(@PathVariable Long tenantId, @PathVariable Long serviceId) {
        return incidentService.findByServiceIdAndTenantId(serviceId, tenantId);
    }
}
