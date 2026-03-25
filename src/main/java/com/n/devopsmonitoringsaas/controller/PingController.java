package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.Ping;
import com.n.devopsmonitoringsaas.service.PingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/tenants/{tenantId}/services/{serviceId}/pings")
@RequiredArgsConstructor
public class PingController {

    private final PingService pingService;

    @GetMapping
    @PreAuthorize("@tenantSecurity.sameTenant(#tenantId) and hasAnyRole('OWNER','ADMIN','MEMBER')")
    public List<Ping> listPings(
            @PathVariable Long tenantId,
            @PathVariable Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Boolean healthy) {
        return pingService.findPings(tenantId, serviceId, from, to, healthy);
    }
}
