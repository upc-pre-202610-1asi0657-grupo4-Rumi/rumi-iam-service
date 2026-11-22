package com.rumi.iam.acceptance;

import com.rumi.iam.infrastructure.security.GatewayJwtDecoder;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps of US14. The gateway is played here as rumi-api-gateway does it: the token is validated with the
 * gateway decoder and its {@code sub} claim is sent as {@code X-User-Id}.
 */
public class DeviceTokenSteps {

    private static final String DEVICE_TOKENS = "/api/v1/device-tokens";

    @Autowired
    private ApiClient api;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${rumi.security.jwt.secret}")
    private String secret;

    private UUID userId;
    private String lastDeviceToken;

    @Given("the user is signed in through the gateway with email {string} and password {string}")
    public void theUserIsSignedInThroughTheGateway(String email, String password) throws Exception {
        api.post("/api/v1/authentication/sign-in", Map.of("email", email, "password", password));
        assertThat(api.response().getStatus()).isEqualTo(200);
        String token = api.body().path("token").asText();
        userId = UUID.fromString(GatewayJwtDecoder.create(secret).decode(token).getSubject());
    }

    @When("the user registers the device token {string} for platform {string}")
    public void theUserRegistersTheDeviceToken(String deviceToken, String platform) throws Exception {
        lastDeviceToken = deviceToken;
        api.post(DEVICE_TOKENS, Map.of("token", deviceToken, "platform", platform),
                Map.of("X-User-Id", userId.toString()));
    }

    @Given("the user registered the device token {string} for platform {string}")
    public void theUserRegisteredTheDeviceToken(String deviceToken, String platform) throws Exception {
        theUserRegistersTheDeviceToken(deviceToken, platform);
        assertThat(api.response().getStatus()).isEqualTo(201);
    }

    @When("a client registers the device token {string} for platform {string} without the gateway header")
    public void aClientRegistersWithoutTheGatewayHeader(String deviceToken, String platform) throws Exception {
        lastDeviceToken = deviceToken;
        api.post(DEVICE_TOKENS, Map.of("token", deviceToken, "platform", platform));
    }

    @Then("the response describes an {string} device without its token")
    public void theResponseDescribesADevice(String platform) throws Exception {
        assertThat(api.body().path("id").asText()).isNotBlank();
        assertThat(api.body().path("platform").asText()).isEqualTo(platform);
        assertThat(api.body().path("registeredAt").asText()).isNotBlank();
        assertThat(api.body().has("token")).isFalse();
        assertThat(api.responseText()).doesNotContain(lastDeviceToken);
    }

    @And("the user has {int} registered device(s)")
    public void theUserHasRegisteredDevices(int count) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM device_tokens WHERE user_id = ?", Integer.class, userId)).isEqualTo(count);
    }
}
