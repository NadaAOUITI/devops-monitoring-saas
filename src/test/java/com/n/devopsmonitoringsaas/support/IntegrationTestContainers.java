package com.n.devopsmonitoringsaas.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Single Postgres + Redis pair for the whole test JVM. Static {@code @Container} fields stop
 * after each test class, so integration tests must share manually started containers.
 */
public final class IntegrationTestContainers {

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("devopsmonitoring_test")
            .withUsername("test")
            .withPassword("test");

    public static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    private IntegrationTestContainers() {
    }
}
