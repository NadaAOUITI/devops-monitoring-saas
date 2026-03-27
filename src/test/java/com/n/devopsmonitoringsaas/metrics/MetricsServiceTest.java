package com.n.devopsmonitoringsaas.metrics;

import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsService")
class MetricsServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    private MeterRegistry registry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        when(serviceRepository.count()).thenReturn(0L);
        registry = new SimpleMeterRegistry();
        metricsService = new MetricsService(registry, serviceRepository);
    }

    @Nested
    @DisplayName("incrementPingsExecuted")
    class Pings {

        @Test
        @DisplayName("increments devops.pings.total counter")
        void incrementsCounter() {
            metricsService.incrementPingsExecuted();
            metricsService.incrementPingsExecuted();

            assertThat(registry.get("devops.pings.total").counter().count()).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("incrementIncidentsOpened")
    class Incidents {

        @Test
        @DisplayName("increments devops.incidents.opened.total counter")
        void incrementsCounter() {
            metricsService.incrementIncidentsOpened();

            assertThat(registry.get("devops.incidents.opened.total").counter().count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("incrementAlertsSent")
    class Alerts {

        @Test
        @DisplayName("increments devops.alerts.sent.total counter")
        void incrementsCounter() {
            metricsService.incrementAlertsSent();

            assertThat(registry.get("devops.alerts.sent.total").counter().count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("updateActiveServicesCount")
    class ActiveServices {

        @Test
        @DisplayName("sets gauge from ServiceRepository.count()")
        void updatesGauge() {
            when(serviceRepository.count()).thenReturn(7L);
            clearInvocations(serviceRepository);
            metricsService.updateActiveServicesCount();

            assertThat(registry.get("devops.active.services").gauge().value()).isEqualTo(7.0);
            verify(serviceRepository).count();
        }
    }

    @Nested
    @DisplayName("recordPingLatency")
    class Latency {

        @Test
        @DisplayName("records a sample on devops.ping.latency timer")
        void recordsTimer() {
            metricsService.recordPingLatency(42L);

            Timer timer = registry.get("devops.ping.latency").timer();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(42.0);
        }
    }

    @Nested
    @DisplayName("constructor")
    class Construction {

        @Test
        @DisplayName("registers meters and initial active-services gauge from repository count")
        void registersMeters() {
            when(serviceRepository.count()).thenReturn(3L);
            MeterRegistry r = new SimpleMeterRegistry();
            new MetricsService(r, serviceRepository);

            assertThat(r.get("devops.pings.total").counter().count()).isEqualTo(0.0);
            assertThat(r.get("devops.active.services").gauge().value()).isEqualTo(3.0);
        }
    }
}
