package com.rumi.iam;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * One PostgreSQL container for the whole test run, shared by every Spring context that imports this class.
 * The database is created from {@code docs/schema.sql} when the container starts, the same file that creates
 * {@code iamdb} in a deployment, and Hibernate runs with {@code ddl-auto=validate}: the tests fail if the schema
 * and the JPA entities disagree.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainer {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("iamdb")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("docs/schema.sql"), "/docker-entrypoint-initdb.d/schema.sql");

    static {
        POSTGRES.start();
    }

    /**
     * No destroy method: a closing context must not stop the container that other cached contexts still use.
     * Testcontainers removes it when the JVM exits.
     */
    @Bean(destroyMethod = "")
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES;
    }
}
