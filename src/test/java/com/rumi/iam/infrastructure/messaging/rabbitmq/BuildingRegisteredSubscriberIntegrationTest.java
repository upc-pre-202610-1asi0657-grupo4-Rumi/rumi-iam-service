package com.rumi.iam.infrastructure.messaging.rabbitmq;

import com.rumi.iam.PostgresTestContainer;
import com.rumi.iam.UserFixtures;
import com.rumi.iam.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The listener on a real RabbitMQ and PostgreSQL. Messages are published exactly as rumi-building-service does:
 * raw JSON on rumi.building-management.events with routing key building-management.building.registered.
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=true",
        "spring.rabbitmq.listener.simple.retry.initial-interval=100ms"
})
@ActiveProfiles("test")
@Import(PostgresTestContainer.class)
class BuildingRegisteredSubscriberIntegrationTest {

    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");
    private static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void startClean() {
        jdbcTemplate.update("DELETE FROM processed_events");
        jdbcTemplate.update("DELETE FROM user_building_ids");
        jdbcTemplate.update("DELETE FROM device_tokens");
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM users");
        while (rabbitTemplate.receive(IamMessagingConfiguration.BUILDING_REGISTERED_DLQ) != null) {
            // drain the dead letters of earlier tests
        }
    }

    @Test
    void linksTheAdministratorWhenBuildingRegisteredArrives() {
        users.save(UserFixtures.administrator());
        UUID eventId = UUID.randomUUID();

        publish(event(eventId, BUILDING_ID, UserFixtures.USER_ID));

        awaitUntil(() -> processed(eventId) == 1);
        assertThat(users.findById(UserFixtures.USER_ID).orElseThrow().getBuildingIds()).containsExactly(BUILDING_ID);
    }

    @Test
    void theSameEventDeliveredTwiceIsAppliedOnce() {
        users.save(UserFixtures.administrator());
        UUID eventId = UUID.randomUUID();
        String event = event(eventId, BUILDING_ID, UserFixtures.USER_ID);

        publish(event);
        publish(event);

        awaitUntil(() -> processed(eventId) == 1);
        // Give the second delivery time to arrive and be discarded.
        sleep(Duration.ofSeconds(1));
        assertThat(processed(eventId)).isEqualTo(1);
        assertThat(users.findById(UserFixtures.USER_ID).orElseThrow().getBuildingIds()).containsExactly(BUILDING_ID);
        assertThat(rabbitTemplate.receive(IamMessagingConfiguration.BUILDING_REGISTERED_DLQ)).isNull();
    }

    @Test
    void anEventForAnUnknownUserGoesToTheDeadLetterQueue() {
        UUID eventId = UUID.randomUUID();
        String event = event(eventId, BUILDING_ID, UUID.randomUUID());

        publish(event);

        Message deadLetter = rabbitTemplate.receive(IamMessagingConfiguration.BUILDING_REGISTERED_DLQ, 10_000);
        assertThat(deadLetter).isNotNull();
        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).isEqualTo(event);
        assertThat(processed(eventId)).isZero();
    }

    @Test
    void aMalformedMessageGoesToTheDeadLetterQueue() {
        publish("{\"eventId\":\"not-a-uuid\"}");

        assertThat(rabbitTemplate.receive(IamMessagingConfiguration.BUILDING_REGISTERED_DLQ, 10_000)).isNotNull();
    }

    private void publish(String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbitTemplate.send(IamMessagingConfiguration.BUILDING_MANAGEMENT_EXCHANGE,
                IamMessagingConfiguration.BUILDING_REGISTERED_ROUTING_KEY,
                new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }

    private static String event(UUID eventId, UUID buildingId, UUID administratorId) {
        return "{\"eventId\":\"" + eventId + "\",\"buildingId\":\"" + buildingId + "\",\"administratorId\":\""
                + administratorId + "\",\"occurredAt\":\"" + Instant.parse("2026-11-28T06:00:00Z") + "\"}";
    }

    private int processed(UUID eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM processed_events WHERE event_id = ?", Integer.class, eventId);
        return count == null ? 0 : count;
    }

    private static void awaitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met within 10 seconds");
            }
            sleep(Duration.ofMillis(100));
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
