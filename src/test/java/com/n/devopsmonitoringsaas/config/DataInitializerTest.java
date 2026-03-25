package com.n.devopsmonitoringsaas.config;

import com.n.devopsmonitoringsaas.entity.AlertChannel;
import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class DataInitializerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("devopsmonitoring_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        registry.add("jwt.secret", () -> "test-jwt-secret-key-min-256-bits-for-hs256-algorithm");
    }

    @Autowired
    private PlanRepository planRepository;

    @Test
    @DisplayName("DataInitializer inserts Free, Pro, Enterprise plans when none exist")
    void insertsPlansOnStartup() {
        List<Plan> plans = planRepository.findAll();

        assertThat(plans).hasSize(3);

        Plan free = plans.stream().filter(p -> "Free".equals(p.getName())).findFirst().orElseThrow();
        assertThat(free.getMaxServices()).isEqualTo(3);
        assertThat(free.getMinPingIntervalSeconds()).isEqualTo(300);
        assertThat(free.getAllowedAlertChannels()).containsExactly(AlertChannel.EMAIL);

        Plan pro = plans.stream().filter(p -> "Pro".equals(p.getName())).findFirst().orElseThrow();
        assertThat(pro.getMaxServices()).isEqualTo(20);
        assertThat(pro.getMinPingIntervalSeconds()).isEqualTo(60);
        assertThat(pro.getAllowedAlertChannels()).containsExactly(AlertChannel.EMAIL);

        Plan enterprise = plans.stream().filter(p -> "Enterprise".equals(p.getName())).findFirst().orElseThrow();
        assertThat(enterprise.getMaxServices()).isEqualTo(100);
        assertThat(enterprise.getMinPingIntervalSeconds()).isEqualTo(10);
        assertThat(enterprise.getAllowedAlertChannels()).containsExactly(AlertChannel.EMAIL);
    }
}
