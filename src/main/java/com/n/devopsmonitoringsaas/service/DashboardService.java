package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.controller.dto.DashboardSummaryItem;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int UPTIME_HOURS = 24;

    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;

    public List<DashboardSummaryItem> getSummary(Long tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }

        Instant since = Instant.now().minusSeconds(UPTIME_HOURS * 3600L);
        List<Object[]> rows = serviceRepository.findDashboardDataByTenantId(tenantId, since);

        return rows.stream()
                .map(this::mapToSummaryItem)
                .collect(Collectors.toList());
    }

    private DashboardSummaryItem mapToSummaryItem(Object[] row) {
        Long serviceId = ((Number) row[0]).longValue();
        String serviceName = (String) row[1];
        String url = (String) row[2];
        Integer maxLatencyMs = row[3] != null ? ((Number) row[3]).intValue() : 2000;
        Integer statusCode = row[4] != null ? ((Number) row[4]).intValue() : null;
        Integer latencyMs = row[5] != null ? ((Number) row[5]).intValue() : null;
        Boolean isHealthy = row[6] != null ? (Boolean) row[6] : null;
        Instant lastChecked = toInstant(row[7]);
        long openIncidents = row[8] != null ? ((Number) row[8]).longValue() : 0L;
        long healthyCount = row[9] != null ? ((Number) row[9]).longValue() : 0L;
        long totalCount = row[10] != null ? ((Number) row[10]).longValue() : 0L;

        String currentStatus = deriveCurrentStatus(isHealthy, statusCode, latencyMs, maxLatencyMs);
        double uptimePercent = totalCount > 0 ? (healthyCount * 100.0 / totalCount) : 100.0;

        return new DashboardSummaryItem(
                serviceId,
                serviceName,
                url,
                currentStatus,
                lastChecked,
                latencyMs,
                openIncidents,
                Math.round(uptimePercent * 100.0) / 100.0
        );
    }

    private Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof Instant i) {
            return i;
        }
        return null;
    }

    private String deriveCurrentStatus(Boolean isHealthy, Integer statusCode, Integer latencyMs, int maxLatencyMs) {
        if (Boolean.TRUE.equals(isHealthy)) {
            return "HEALTHY";
        }
        if (isHealthy == null && statusCode == null && latencyMs == null) {
            return "HEALTHY"; // no pings yet
        }
        int lat = latencyMs != null ? latencyMs : 0;
        if (lat > maxLatencyMs) {
            return "SLOW";
        }
        if (statusCode == null || statusCode >= 500) {
            return "DOWN";
        }
        if (statusCode >= 400) {
            return "DEGRADED";
        }
        return "DOWN";
    }
}
