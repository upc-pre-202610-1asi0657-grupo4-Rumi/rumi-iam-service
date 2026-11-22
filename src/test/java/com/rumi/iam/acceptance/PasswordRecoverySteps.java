package com.rumi.iam.acceptance;

import com.rumi.iam.MutableClock;
import com.rumi.iam.application.CapturingPasswordResetNotifier;
import com.rumi.iam.domain.service.ResetTokenGenerator;
import com.rumi.iam.infrastructure.persistence.SpringDataUserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps of US03. The raw token is read from the captured link, as the user would read it from the e-mail,
 * never from the database.
 */
public class PasswordRecoverySteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private CapturingPasswordResetNotifier notifier;

    @Autowired
    private MutableClock clock;

    @Autowired
    private SpringDataUserRepository users;

    @Autowired
    private ResetTokenGenerator resetTokenGenerator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String lastNewPassword;

    @When("the user asks for a password reset link for {string}")
    public void theUserAsksForAResetLink(String email) throws Exception {
        api.post("/api/v1/authentication/forgot-password", Map.of("email", email));
    }

    @Given("the user asked for a password reset link for {string}")
    public void theUserAskedForAResetLink(String email) throws Exception {
        theUserAsksForAResetLink(email);
        assertThat(api.response().getStatus()).isEqualTo(202);
        assertThat(notifier.last()).isPresent();
    }

    @When("the user resets the password to {string} with the link")
    public void theUserResetsThePassword(String newPassword) throws Exception {
        lastNewPassword = newPassword;
        api.post("/api/v1/authentication/reset-password",
                Map.of("token", rawTokenOfTheLink(), "newPassword", newPassword));
    }

    @Given("the user reset the password to {string} with the link")
    public void theUserResetThePassword(String newPassword) throws Exception {
        theUserResetsThePassword(newPassword);
        assertThat(api.response().getStatus()).isEqualTo(204);
    }

    @Given("{int} minutes have passed")
    public void minutesHavePassed(int minutes) {
        clock.advance(Duration.ofMinutes(minutes));
    }

    @Then("the response body is empty")
    public void theResponseBodyIsEmpty() throws Exception {
        assertThat(api.responseText()).isEmpty();
    }

    @And("a reset link is sent to {string}")
    public void aResetLinkIsSent(String email) {
        assertThat(users.findByEmail(email)).hasValueSatisfying(user ->
                assertThat(notifier.sent()).singleElement().satisfies(link -> {
                    assertThat(link.userId()).isEqualTo(user.getId());
                    assertThat(link.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(30)));
                }));
    }

    @And("no reset link is sent")
    public void noResetLinkIsSent() {
        assertThat(notifier.sent()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM password_reset_tokens", Integer.class)).isZero();
    }

    @And("only the hash of the link is stored")
    public void onlyTheHashOfTheLinkIsStored() {
        String rawToken = rawTokenOfTheLink();
        List<String> storedHashes = jdbcTemplate.queryForList("SELECT token_hash FROM password_reset_tokens", String.class);

        assertThat(storedHashes).containsExactly(resetTokenGenerator.hash(rawToken));
        assertThat(storedHashes.get(0)).isNotEqualTo(rawToken);
    }

    @And("the response does not contain the link or the password")
    public void theResponseDoesNotContainTheLinkOrThePassword() throws Exception {
        assertThat(api.responseText()).doesNotContain(rawTokenOfTheLink()).doesNotContain(lastNewPassword);
    }

    private String rawTokenOfTheLink() {
        return notifier.last().orElseThrow(() -> new AssertionError("No reset link was sent")).rawToken();
    }
}
