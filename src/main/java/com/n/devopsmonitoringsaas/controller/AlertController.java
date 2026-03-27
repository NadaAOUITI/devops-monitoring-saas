package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.Alert;
import com.n.devopsmonitoringsaas.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants/{tenantId}")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN','ROLE_MEMBER')")
    public List<Alert> listAlerts(@PathVariable Long tenantId) {
        return alertService.findByTenantId(tenantId);
    }

    @GetMapping("/alerts/{alertId}")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN','ROLE_MEMBER')")
    public Alert getAlert(@PathVariable Long tenantId, @PathVariable Long alertId) {
        return alertService.findByIdAndTenantId(alertId, tenantId);
    }

    @PutMapping("/alerts/{alertId}/acknowledge")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN')")
    public Alert acknowledgeAlert(@PathVariable Long tenantId, @PathVariable Long alertId) {
        return alertService.acknowledge(alertId, tenantId);
    }

    @GetMapping("/services/{serviceId}/alerts")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN','ROLE_MEMBER')")
    public List<Alert> listServiceAlerts(@PathVariable Long tenantId, @PathVariable Long serviceId) {
        return alertService.findByServiceIdAndTenantId(serviceId, tenantId);
    }
}
