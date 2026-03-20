package com.n.devopsmonitoringsaas.controller.dto;

import java.time.Instant;

public record DashboardSummaryItem(
        Long serviceId,
        String serviceName,
        String url,
        String currentStatus,
        Instant lastChecked,
        Integer latencyMs,
        long openIncidents,
        double uptimePercent
) {}
