package com.rumi.iam.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.rumi.iam.MutableClock;
import com.rumi.iam.infrastructure.messaging.rabbitmq.BuildingRegisteredSubscriber;
import com.rumi.iam.infrastructure.persistence.SpringDataUserRepository;
import com.rumi.iam.infrastructure.security.GatewayJwtDecoder;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BuildingRegistered events are handed to the RabbitMQ listener directly, with the JSON that Building Management
 * publishes; BuildingRegisteredSubscriberIntegrationTest covers the same listener on a real broker.
 */
public class BuildingMembershipSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private BuildingRegisteredSubscriber subscriber;

    @Autowired
    private SpringDataUserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock clock;

    @Value("${rumi.security.jwt.secret}")
    private String secret;

    private String userId;
    private String token;
    private Instant signedInAt;
    private String lastEvent;
    private UUID lastEventId;

    @Given("the administrator {string} with password {string} has signed in")
    public void theAdministratorHasSignedIn(String email, String password) throws Exception {
        api.post("/api/v1/authentication/sign-up", Map.of("email", email, "password", password, "fullName", "Ana Torres"));
        assertThat(api.response().getStatus()).isEqualTo(201);
        api.post("/api/v1/authentication/sign-in", Map.of("email", email, "password", password));
        assertThat(api.response().getStatus()).isEqualTo(200);
        userId = api.body().path("userId").asText();
        token = api.body().path("token").asText();
        signedInAt = GatewayJwtDecoder.create(secret).decode(token).getIssuedAt();
    }

    @When("Building Management reports that the administrator registered the building {string}")
    public void buildingManagementReportsTheBuilding(String buildingId) {
        lastEventId = UUID.randomUUID();
        lastEvent = "{\"eventId\":\"" + lastEventId + "\",\"buildingId\":\"" + buildingId
                + "\",\"administratorId\":\"" + userId + "\",\"occurredAt\":\"" + clock.instant() + "\"}";
        deliver(lastEvent);
    }

    @When("the same BuildingRegistered event is delivered again")
    public void theSameEventIsDeliveredAgain() {
        deliver(lastEvent);
    }

    @When("the user asks for a new token")
    public void theUserAsksForANewToken() throws Exception {
        api.post("/api/v1/users/me/token", Map.of(),
                Map.of("X-User-Id", userId, "Authorization", "Bearer " + token));
    }

    @When("the user renews the token every 10 minutes for 12 hours")
    public void theUserRenewsTheTokenForTwelveHours() throws Exception {
        for (int renewal = 0; renewal < 72; renewal++) {
            clock.advance(Duration.ofMinutes(10));
            theUserAsksForANewToken();
            assertThat(api.response().getStatus()).as("renewal %d", renewal + 1).isEqualTo(200);
            token = api.body().path("token").asText();
        }
    }

    @When("{int} more minute passes and the user asks for a new token")
    public void moreMinutesPassAndTheUserAsksForANewToken(int minutes) throws Exception {
        clock.advance(Duration.ofMinutes(minutes));
        theUserAsksForANewToken();
    }

    @Then("the response lists the building {string}")
    public void theResponseListsTheBuilding(String buildingId) throws Exception {
        JsonNode buildingIds = api.body().path("buildingIds");
        assertThat(buildingIds).hasSize(1);
        assertThat(buildingIds.get(0).asText()).isEqualTo(buildingId);
    }

    @And("the API gateway accepts the new token with the role {string} and the building {string}")
    public void theGatewayAcceptsTheNewToken(String role, String buildingId) throws Exception {
        Jwt jwt = GatewayJwtDecoder.create(secret).decode(api.body().path("token").asText());

        assertThat(jwt.getSubject()).isEqualTo(userId);
        assertThat(jwt.getClaimAsString("role")).isEqualTo(role);
        assertThat(jwt.getClaimAsStringList("buildingIds")).containsExactly(buildingId);
    }

    @And("the new token keeps the time of the password sign-in")
    public void theNewTokenKeepsTheSignInTime() throws Exception {
        Jwt jwt = GatewayJwtDecoder.create(secret).decode(api.body().path("token").asText());

        assertThat(((Number) jwt.getClaim("auth_time")).longValue()).isEqualTo(signedInAt.getEpochSecond());
    }

    @Then("the administrator belongs only to the building {string}")
    public void theAdministratorBelongsOnlyTo(String buildingId) {
        assertThat(users.findById(UUID.fromString(userId)).orElseThrow().getBuildingIds())
                .containsExactly(UUID.fromString(buildingId));
    }

    @And("the event is recorded once as processed")
    public void theEventIsRecordedOnce() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM processed_events WHERE event_id = ?", Integer.class, lastEventId);
        assertThat(count).isEqualTo(1);
    }

    private void deliver(String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        subscriber.onBuildingRegistered(new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }
}
