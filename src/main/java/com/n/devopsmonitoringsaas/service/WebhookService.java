package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Alert;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.repository.AlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Slf4j
public class WebhookService {

    private final AlertRepository alertRepository;
    private final RestTemplate webhookRestTemplate;

    public WebhookService(AlertRepository alertRepository,
                          @Qualifier("webhookRestTemplate") RestTemplate webhookRestTemplate) {
        this.alertRepository = alertRepository;
        this.webhookRestTemplate = webhookRestTemplate;
    }

    @Async
    public void sendAlert(Alert alert) {
        if (alert == null || alert.getId() == null) {
            return;
        }

        Alert loaded = alertRepository.findWithDetailsById(alert.getId()).orElse(null);
        if (loaded == null) {
            log.warn("Webhook skipped: alert {} not found", alert.getId());
            return;
        }

        Tenant tenant = loaded.getPing().getService().getTenant();
        String webhookUrl = tenant.getWebhookUrl();
        if (!StringUtils.hasText(webhookUrl)) {
            return;
        }

        String incidentCause = loaded.getPing().getIncident() != null
                ? loaded.getPing().getIncident().getCause().name()
                : loaded.getType().name();

        WebhookPayload payload = new WebhookPayload(
                loaded.getId(),
                loaded.getPing().getService().getName(),
                loaded.getPing().getService().getUrl(),
                incidentCause,
                loaded.getMessage(),
                loaded.getSentAt() != null ? loaded.getSentAt() : Instant.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WebhookPayload> entity = new HttpEntity<>(payload, headers);

        try {
            webhookRestTemplate.postForEntity(webhookUrl, entity, String.class);
            log.info("Webhook delivered for alert id={}", loaded.getId());
        } catch (Exception e) {
            log.warn("Webhook delivery failed for alert id={}: {}", loaded.getId(), e.getMessage());
        }
    }

    public record WebhookPayload(
            Long alertId,
            String serviceName,
            String serviceUrl,
            String incidentCause,
            String message,
            Instant timestamp
    ) {}
}
