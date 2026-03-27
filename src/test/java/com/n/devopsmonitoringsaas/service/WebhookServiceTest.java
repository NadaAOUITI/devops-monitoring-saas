package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Alert;
import com.n.devopsmonitoringsaas.entity.AlertType;
import com.n.devopsmonitoringsaas.entity.Incident;
import com.n.devopsmonitoringsaas.entity.IncidentCause;
import com.n.devopsmonitoringsaas.entity.IncidentStatus;
import com.n.devopsmonitoringsaas.entity.Ping;
import com.n.devopsmonitoringsaas.entity.Service;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService")
class WebhookServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private RestTemplate webhookRestTemplate;

    private WebhookService webhookService;

    private Tenant tenant;
    private Service service;
    private Ping ping;
    private Incident incident;
    private Alert alert;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(alertRepository, webhookRestTemplate);

        tenant = Tenant.builder()
                .id(10L)
                .name("T")
                .webhookUrl("https://hooks.example.com/r")
                .build();

        service = Service.builder()
                .id(20L)
                .name("API")
                .url("https://api.example.com")
                .pingIntervalSeconds(60)
                .tenant(tenant)
                .build();

        incident = Incident.builder()
                .id(30L)
                .service(service)
                .tenant(tenant)
                .status(IncidentStatus.OPEN)
                .cause(IncidentCause.DOWN)
                .openedAt(Instant.now())
                .build();

        ping = Ping.builder()
                .id(40L)
                .service(service)
                .timestamp(Instant.now())
                .statusCode(500)
                .latencyMs(10)
                .isHealthy(false)
                .incident(incident)
                .build();

        alert = Alert.builder()
                .id(50L)
                .ping(ping)
                .type(AlertType.DOWN)
                .message("down")
                .sentAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("sendAlert")
    class SendAlert {

        @Test
        @DisplayName("POSTs JSON payload when webhook URL is configured")
        void validWebhookUrl_postsPayload() {
            when(alertRepository.findWithDetailsById(50L)).thenReturn(Optional.of(alert));
            when(webhookRestTemplate.postForEntity(eq("https://hooks.example.com/r"), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("ok"));

            webhookService.sendAlert(alert);

            verify(webhookRestTemplate).postForEntity(
                    eq("https://hooks.example.com/r"),
                    argThat(entity -> {
                        if (!(entity instanceof HttpEntity<?> httpEntity)) {
                            return false;
                        }
                        WebhookService.WebhookPayload p = (WebhookService.WebhookPayload) httpEntity.getBody();
                        return p != null
                                && p.alertId().equals(50L)
                                && "API".equals(p.serviceName())
                                && "DOWN".equals(p.incidentCause());
                    }),
                    eq(String.class));
        }

        @Test
        @DisplayName("does not call RestTemplate when webhook URL is blank")
        void blankWebhookUrl_doesNotSend() {
            tenant.setWebhookUrl(" ");
            when(alertRepository.findWithDetailsById(50L)).thenReturn(Optional.of(alert));

            webhookService.sendAlert(alert);

            verify(webhookRestTemplate, never()).postForEntity(any(), any(), any());
        }

        @Test
        @DisplayName("does not call RestTemplate when webhook URL is null")
        void nullWebhookUrl_doesNotSend() {
            tenant.setWebhookUrl(null);
            when(alertRepository.findWithDetailsById(50L)).thenReturn(Optional.of(alert));

            webhookService.sendAlert(alert);

            verify(webhookRestTemplate, never()).postForEntity(any(), any(), any());
        }

        @Test
        @DisplayName("swallows connection timeout and does not rethrow")
        void connectionTimeout_logsAndDoesNotThrow() {
            when(alertRepository.findWithDetailsById(50L)).thenReturn(Optional.of(alert));
            when(webhookRestTemplate.postForEntity(any(), any(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("timed out", new SocketTimeoutException("timeout")));

            webhookService.sendAlert(alert);

            verify(webhookRestTemplate).postForEntity(eq("https://hooks.example.com/r"), any(), eq(String.class));
        }

        @Test
        @DisplayName("swallows HTTP 4xx from webhook endpoint")
        void clientError_logsAndDoesNotThrow() {
            when(alertRepository.findWithDetailsById(50L)).thenReturn(Optional.of(alert));
            when(webhookRestTemplate.postForEntity(any(), any(), eq(String.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

            webhookService.sendAlert(alert);

            verify(webhookRestTemplate).postForEntity(eq("https://hooks.example.com/r"), any(), eq(String.class));
        }

        @Test
        @DisplayName("swallows HTTP 5xx from webhook endpoint")
        void serverError_logsAndDoesNotThrow() {
            when(alertRepository.findWithDetailsById(50L)).thenReturn(Optional.of(alert));
            when(webhookRestTemplate.postForEntity(any(), any(), eq(String.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

            webhookService.sendAlert(alert);

            verify(webhookRestTemplate).postForEntity(eq("https://hooks.example.com/r"), any(), eq(String.class));
        }

        @Test
        @DisplayName("returns early when alert id is null")
        void nullAlertId_returnsEarly() {
            Alert a = Alert.builder().id(null).build();

            webhookService.sendAlert(a);

            verify(alertRepository, never()).findWithDetailsById(any());
        }
    }
}
