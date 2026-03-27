package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Incident;
import com.n.devopsmonitoringsaas.metrics.MetricsService;
import com.n.devopsmonitoringsaas.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final MetricsService metricsService;

    public List<Incident> findByTenantId(Long tenantId) {
        return incidentRepository.findByTenantIdOrderByOpenedAtDesc(tenantId);
    }

    public List<Incident> findByServiceIdAndTenantId(Long serviceId, Long tenantId) {
        return incidentRepository.findByServiceIdAndTenantIdOrderByOpenedAtDesc(serviceId, tenantId);
    }

    public Incident findByIdAndTenantId(Long id, Long tenantId) {
        return incidentRepository.findById(id)
                .filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));
    }

    @Transactional
    public Incident saveNewOpenIncident(Incident incident) {
        Incident saved = incidentRepository.save(incident);
        metricsService.incrementIncidentsOpened();
        return saved;
    }
}
