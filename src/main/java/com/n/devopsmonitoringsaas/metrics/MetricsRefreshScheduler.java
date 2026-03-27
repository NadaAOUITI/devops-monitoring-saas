package com.n.devopsmonitoringsaas.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic gauge refresh; excluded from {@code test} profile so integration tests do not compete for DB connections.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MetricsRefreshScheduler {

    private final MetricsService metricsService;

    @Scheduled(fixedRateString = "${devops.metrics.active-services.refresh-ms:60000}")
    public void scheduledActiveServicesRefresh() {
        metricsService.updateActiveServicesCount();
    }
}
