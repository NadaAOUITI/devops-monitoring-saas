package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.repository.PlanRepository;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TenantControllerTest {

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
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PlanRepository planRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        Plan plan = planRepository.findAll().stream().findFirst().orElseThrow();
        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant")
                .plan(plan)
                .build());
    }

    @Test
    @DisplayName("GET /tenants/{id} returns tenant when found")
    @WithMockUser
    void getTenant_returnsTenant_whenFound() throws Exception {
        mockMvc.perform(get("/tenants/{tenantId}", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenant.getId()))
                .andExpect(jsonPath("$.name").value("Test Tenant"));
    }

    @Test
    @DisplayName("GET /tenants/{id} returns 404 when not found")
    @WithMockUser
    void getTenant_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/tenants/{tenantId}", 99999L))
                .andExpect(status().isNotFound());
    }
}
