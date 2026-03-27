package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Alert;
import com.n.devopsmonitoringsaas.metrics.MetricsService;
import com.n.devopsmonitoringsaas.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final MetricsService metricsService;

    public List<Alert> findByTenantId(Long tenantId) {
        return alertRepository.findByPingServiceTenantIdOrderBySentAtDesc(tenantId);
    }

    public List<Alert> findByServiceIdAndTenantId(Long serviceId, Long tenantId) {
        return alertRepository.findByPingServiceIdAndPingServiceTenantIdOrderBySentAtDesc(serviceId, tenantId);
    }

    public Alert findByIdAndTenantId(Long id, Long tenantId) {
        return alertRepository.findByIdAndPingServiceTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
    }

    @Transactional
    public Alert acknowledge(Long id, Long tenantId) {
        Alert alert = findByIdAndTenantId(id, tenantId);
        alert.setAcknowledgedAt(Instant.now());
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert saveNewAlert(Alert alert) {
        Alert saved = alertRepository.save(alert);
        metricsService.incrementAlertsSent();
        return saved;
    }
}
