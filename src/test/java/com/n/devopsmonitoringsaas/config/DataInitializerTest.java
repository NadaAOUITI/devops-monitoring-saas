package com.n.devopsmonitoringsaas.config;

import com.n.devopsmonitoringsaas.entity.AlertChannel;
import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertySource;
import com.n.devopsmonitoringsaas.support.IntegrationTestContainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataInitializerTest {

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
        assertThat(pro.getAllowedAlertChannels()).containsExactly(AlertChannel.EMAIL, AlertChannel.WEBHOOK);

        Plan enterprise = plans.stream().filter(p -> "Enterprise".equals(p.getName())).findFirst().orElseThrow();
        assertThat(enterprise.getMaxServices()).isEqualTo(100);
        assertThat(enterprise.getMinPingIntervalSeconds()).isEqualTo(10);
        assertThat(enterprise.getAllowedAlertChannels()).containsExactly(AlertChannel.EMAIL, AlertChannel.WEBHOOK);
    }
}
