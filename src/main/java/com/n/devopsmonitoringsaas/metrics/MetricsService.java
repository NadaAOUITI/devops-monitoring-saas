package com.n.devopsmonitoringsaas.metrics;

import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsService {

    private static final Tags APPLICATION_TAGS = Tags.of("application", "devops-monitoring");

    private final Counter totalPingsExecuted;
    private final Counter totalIncidentsOpened;
    private final Counter totalAlertsSent;
    private final AtomicLong activeServicesMonitored;
    private final Timer pingLatency;
    private final ServiceRepository serviceRepository;

    public MetricsService(MeterRegistry registry, ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
        this.activeServicesMonitored = new AtomicLong(0);

        this.totalPingsExecuted = Counter.builder("devops.pings.total")
                .description("Total number of pings executed")
                .tags(APPLICATION_TAGS)
                .register(registry);

        this.totalIncidentsOpened = Counter.builder("devops.incidents.opened.total")
                .description("Total number of incidents opened")
                .tags(APPLICATION_TAGS)
                .register(registry);

        this.totalAlertsSent = Counter.builder("devops.alerts.sent.total")
                .description("Total number of alerts sent")
                .tags(APPLICATION_TAGS)
                .register(registry);

        this.pingLatency = Timer.builder("devops.ping.latency")
                .description("Ping execution time")
                .tags(APPLICATION_TAGS)
                .register(registry);

        Gauge.builder("devops.active.services", activeServicesMonitored, AtomicLong::doubleValue)
                .description("Current count of active services across all tenants")
                .tags(APPLICATION_TAGS)
                .register(registry);

        updateActiveServicesCount();
    }

    public void incrementPingsExecuted() {
        totalPingsExecuted.increment();
    }

    public void incrementIncidentsOpened() {
        totalIncidentsOpened.increment();
    }

    public void incrementAlertsSent() {
        totalAlertsSent.increment();
    }

    public void recordPingLatency(long durationMs) {
        pingLatency.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void updateActiveServicesCount() {
        activeServicesMonitored.set(serviceRepository.count());
    }
}
