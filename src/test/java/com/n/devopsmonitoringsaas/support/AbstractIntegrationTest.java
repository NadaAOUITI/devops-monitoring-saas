package com.n.devopsmonitoringsaas.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Shared PostgreSQL + Redis for {@link SpringBootTest} integration tests with {@link MockMvc}.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

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
    private WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }
}
