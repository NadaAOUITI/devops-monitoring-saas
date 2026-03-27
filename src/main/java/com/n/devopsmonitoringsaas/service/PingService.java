package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.*;
import com.n.devopsmonitoringsaas.metrics.MetricsService;
import com.n.devopsmonitoringsaas.repository.IncidentRepository;
import com.n.devopsmonitoringsaas.repository.PingRepository;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class PingService {

    private final RestTemplate restTemplate;
    private final PingRepository pingRepository;
    private final ServiceRepository serviceRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;
    private final AlertService alertService;
    private final WebhookService webhookService;
    private final MetricsService metricsService;

    @Transactional
    public Ping ping(Service service) {
        String url = service.getUrl();
        long startMs = System.currentTimeMillis();

        Integer statusCode = null;
        boolean isHealthy = false;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            statusCode = response.getStatusCode().value();
            isHealthy = statusCode >= 200 && statusCode < 300;
        } catch (RestClientResponseException e) {
            statusCode = e.getStatusCode().value();
            isHealthy = false;
            log.warn("Ping returned {} for service {} (id={})", statusCode, service.getName(), service.getId());
        } catch (Exception e) {
            log.warn("Ping failed for service {} (id={}): {}", service.getName(), service.getId(), e.getMessage());
            isHealthy = false;
        }

        long latencyMs = System.currentTimeMillis() - startMs;
        metricsService.recordPingLatency(latencyMs);

        Service managedService = serviceRepository.findById(service.getId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + service.getId()));
        Ping ping = Ping.builder()
                .service(managedService)
                .timestamp(Instant.now())
                .statusCode(statusCode)
                .latencyMs((int) latencyMs)
                .isHealthy(isHealthy)
                .build();

        ping = pingRepository.save(ping);

        if (!isHealthy) {
            handleFailedPing(ping, managedService, statusCode, (int) latencyMs);
        } else {
            handleSuccessfulPing(managedService);
        }

        metricsService.incrementPingsExecuted();
        return ping;
    }

    public List<Ping> findPings(Long tenantId, Long serviceId, Instant from, Instant to, Boolean healthy) {
        if (!serviceRepository.existsByIdAndTenantId(serviceId, tenantId)) {
            throw new IllegalArgumentException("Service not found: " + serviceId);
        }
        return pingRepository.findByServiceIdWithFilters(serviceId, from, to, healthy);
    }

    private void handleFailedPing(Ping ping, Service service, Integer statusCode, int latencyMs) {
        Incident openIncident = incidentRepository.findByServiceIdAndStatus(service.getId(), IncidentStatus.OPEN)
                .orElse(null);

        if (openIncident != null) {
            ping.setIncident(openIncident);
            pingRepository.save(ping);
        } else {
            int maxLatencyMs = service.getMaxLatencyMs() != null ? service.getMaxLatencyMs() : 2000;
            IncidentCause cause = deriveCause(statusCode, latencyMs, maxLatencyMs);
            Incident incident = Incident.builder()
                    .service(service)
                    .tenant(service.getTenant())
                    .status(IncidentStatus.OPEN)
                    .cause(cause)
                    .openedAt(Instant.now())
                    .closedAt(null)
                    .build();
            incident = incidentService.saveNewOpenIncident(incident);
            ping.setIncident(incident);
            pingRepository.save(ping);

            Alert alert = Alert.builder()
                    .ping(ping)
                    .type(AlertType.valueOf(cause.name()))
                    .message(buildIncidentAlertMessage(service, cause))
                    .sentAt(Instant.now())
                    .build();
            Alert savedAlert = alertService.saveNewAlert(alert);
            webhookService.sendAlert(savedAlert);
        }
    }

    private String buildIncidentAlertMessage(Service service, IncidentCause cause) {
        return "Incident opened for service '%s' (id=%d): %s".formatted(
                service.getName(), service.getId(), cause.name());
    }

    private IncidentCause deriveCause(Integer statusCode, int latencyMs, int maxLatencyMs) {
        if (latencyMs > maxLatencyMs) {
            return IncidentCause.SLOW;
        }
        if (statusCode == null || statusCode >= 500) {
            return IncidentCause.DOWN;
        }
        if (statusCode >= 400) {
            return IncidentCause.DEGRADED;
        }
        return IncidentCause.DOWN; // fallback for other failures
    }

    private void handleSuccessfulPing(Service service) {
        incidentRepository.findByServiceIdAndStatus(service.getId(), IncidentStatus.OPEN)
                .ifPresent(incident -> {
                    incident.setStatus(IncidentStatus.CLOSED);
                    incident.setClosedAt(Instant.now());
                    incidentRepository.save(incident);
                });
    }
}
