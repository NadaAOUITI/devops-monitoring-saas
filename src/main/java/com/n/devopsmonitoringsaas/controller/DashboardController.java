package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.controller.dto.DashboardSummaryItem;
import com.n.devopsmonitoringsaas.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants/{tenantId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public List<DashboardSummaryItem> getSummary(@PathVariable Long tenantId) {
        return dashboardService.getSummary(tenantId);
    }
}
