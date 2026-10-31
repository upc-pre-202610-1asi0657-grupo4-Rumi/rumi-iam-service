package com.rumi.iam.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumi.iam.infrastructure.persistence.SpringDataUserRepository;
import com.rumi.iam.infrastructure.security.GatewayJwtDecoder;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class AuthenticationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository users;

    @Value("${rumi.security.jwt.secret}")
    private String secret;

    private String lastPassword;
    private MockHttpServletResponse response;

    @Before
    public void startWithNoUsers() {
        users.deleteAll();
    }

    @Given("an administrator is registered with email {string} and password {string}")
    public void anAdministratorIsRegistered(String email, String password) throws Exception {
        aVisitorSignsUp(email, password, "Ana Torres");
        assertThat(response.getStatus()).isEqualTo(201);
    }

    @When("a visitor signs up with email {string}, password {string} and full name {string}")
    public void aVisitorSignsUp(String email, String password, String fullName) throws Exception {
        lastPassword = password;
        send("/api/v1/authentication/sign-up", Map.of("email", email, "password", password, "fullName", fullName));
    }

    @When("the user signs in with email {string} and password {string}")
    public void theUserSignsIn(String email, String password) throws Exception {
        lastPassword = password;
        send("/api/v1/authentication/sign-in", Map.of("email", email, "password", password));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(response.getStatus()).isEqualTo(status);
    }

    @And("the response has the role {string}")
    public void theResponseHasTheRole(String role) throws Exception {
        assertThat(body().path("role").asText()).isEqualTo(role);
    }

    @And("the response does not contain the password")
    public void theResponseDoesNotContainThePassword() throws Exception {
        assertThat(body().has("password")).isFalse();
        assertThat(body().has("passwordHash")).isFalse();
        assertThat(responseText()).doesNotContain(lastPassword);
    }

    @And("the password of {string} is stored only as a BCrypt hash")
    public void thePasswordIsStoredOnlyAsABCryptHash(String email) {
        assertThat(users.findByEmail(email)).hasValueSatisfying(user -> assertThat(user.getPasswordHash())
                .startsWith("$2")
                .hasSize(60)
                .doesNotContain(lastPassword));
    }

    @And("the response is a problem detail with the message {string}")
    public void theResponseIsAProblemDetail(String message) throws Exception {
        assertThat(MediaType.parseMediaType(response.getContentType()))
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(body().path("status").asInt()).isEqualTo(response.getStatus());
        assertThat(body().path("detail").asText()).isEqualTo(message);
    }

    @And("the field {string} is reported with the message {string}")
    public void theFieldIsReported(String field, String message) throws Exception {
        assertThat(body().path("errors").path(field).asText()).isEqualTo(message);
    }

    @And("the response contains a bearer token")
    public void theResponseContainsABearerToken() throws Exception {
        assertThat(body().path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body().path("token").asText()).isNotBlank();
        assertThat(body().path("expiresAt").asText()).isNotBlank();
    }

    @And("the API gateway accepts the token with the role {string} and no buildings")
    public void theGatewayAcceptsTheToken(String role) throws Exception {
        Jwt jwt = GatewayJwtDecoder.create(secret).decode(body().path("token").asText());

        assertThat(jwt.getSubject()).isEqualTo(body().path("userId").asText());
        assertThat(jwt.getClaimAsString("role")).isEqualTo(role);
        assertThat(jwt.getClaimAsStringList("buildingIds")).isEmpty();
        assertThat(jwt.getExpiresAt()).isNotNull();
    }

    private void send(String path, Map<String, String> body) throws Exception {
        response = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn()
                .getResponse();
    }

    private JsonNode body() throws Exception {
        return objectMapper.readTree(responseText());
    }

    private String responseText() throws Exception {
        return response.getContentAsString(StandardCharsets.UTF_8);
    }
}
