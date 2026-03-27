package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Alert;
import com.n.devopsmonitoringsaas.entity.AlertType;
import com.n.devopsmonitoringsaas.entity.Incident;
import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.entity.Service;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.repository.AlertRepository;
import com.n.devopsmonitoringsaas.repository.IncidentRepository;
import com.n.devopsmonitoringsaas.repository.PingRepository;
import com.n.devopsmonitoringsaas.repository.PlanRepository;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import com.n.devopsmonitoringsaas.support.IntegrationTestContainers;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PingServiceIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", IntegrationTestContainers.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", IntegrationTestContainers.POSTGRES::getUsername);
        registry.add("spring.datasource.password", IntegrationTestContainers.POSTGRES::getPassword);
        registry.add("spring.data.redis.host", IntegrationTestContainers.REDIS::getHost);
        registry.add("spring.data.redis.port", () -> IntegrationTestContainers.REDIS.getMappedPort(6379).toString());
        registry.add("jwt.secret", () -> "test-jwt-secret-key-min-256-bits-for-hs256-algorithm");
    }

    @Autowired
    private PingService pingService;

    @Autowired
    private PingRepository pingRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    private MockWebServer mockWebServer;
    private Plan plan;
    private Tenant tenant;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        plan = planRepository.save(Plan.builder()
                .name("Test Plan")
                .maxServices(10)
                .minPingIntervalSeconds(60)
                .build());

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant")
                .plan(plan)
                .build());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Nested
    @DisplayName("ping")
    class PingTests {

        @Test
        @DisplayName("saves Ping with correct status code, latency, and isHealthy when URL returns 2xx")
        void savesPingWithCorrectValues_whenHealthy() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("OK"));

            String url = mockWebServer.url("/").toString();
            Service service = serviceRepository.save(Service.builder()
                    .name("Healthy API")
                    .url(url)
                    .pingIntervalSeconds(60)
                    .tenant(tenant)
                    .build());

            var result = pingService.ping(service);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getIsHealthy()).isTrue();
            assertThat(result.getLatencyMs()).isGreaterThanOrEqualTo(0);
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.getService().getId()).isEqualTo(service.getId());

            var saved = pingRepository.findAll();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getStatusCode()).isEqualTo(200);
            assertThat(saved.get(0).getIsHealthy()).isTrue();
        }

        @Test
        @DisplayName("saves Ping with isHealthy false when URL returns 5xx")
        void savesPingWithIsHealthyFalse_whenServerError() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

            String url = mockWebServer.url("/").toString();
            Service service = serviceRepository.save(Service.builder()
                    .name("Unhealthy API")
                    .url(url)
                    .pingIntervalSeconds(60)
                    .tenant(tenant)
                    .build());

            var result = pingService.ping(service);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCode()).isEqualTo(500);
            assertThat(result.getIsHealthy()).isFalse();
            assertThat(result.getLatencyMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("saves Ping with isHealthy false when URL returns 4xx")
        void savesPingWithIsHealthyFalse_whenClientError() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

            String url = mockWebServer.url("/").toString();
            Service service = serviceRepository.save(Service.builder()
                    .name("NotFound API")
                    .url(url)
                    .pingIntervalSeconds(60)
                    .tenant(tenant)
                    .build());

            var result = pingService.ping(service);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getIsHealthy()).isFalse();
        }

        @Test
        @DisplayName("creates Alert when new OPEN incident is created")
        void createsAlertWhenNewIncidentOpens() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

            String url = mockWebServer.url("/").toString();
            Service service = serviceRepository.save(Service.builder()
                    .name("Down API")
                    .url(url)
                    .pingIntervalSeconds(60)
                    .tenant(tenant)
                    .build());

            var result = pingService.ping(service);

            assertThat(result).isNotNull();
            assertThat(result.getIsHealthy()).isFalse();
            assertThat(result.getIncident()).isNotNull();

            List<Incident> incidents = incidentRepository.findAll();
            assertThat(incidents).hasSize(1);
            assertThat(incidents.get(0).getCause().name()).isEqualTo("DOWN");

            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            Alert alert = alerts.get(0);
            assertThat(alert.getType()).isEqualTo(AlertType.DOWN);
            assertThat(alert.getPing().getId()).isEqualTo(result.getId());
            assertThat(alert.getMessage()).contains("Incident opened").contains("Down API").contains("DOWN");
            assertThat(alert.getSentAt()).isNotNull();
        }
    }
}
