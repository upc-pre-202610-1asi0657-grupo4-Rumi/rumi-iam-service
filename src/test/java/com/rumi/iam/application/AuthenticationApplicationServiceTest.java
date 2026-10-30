package com.rumi.iam.application;

import com.rumi.iam.UserFixtures;
import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.PasswordPolicy;
import com.rumi.iam.domain.model.Role;
import com.rumi.iam.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("Enabled by the next feat commit")
class AuthenticationApplicationServiceTest {

    private InMemoryUserRepository userRepository;
    private FakePasswordHasher passwordHasher;
    private AuthenticationApplicationService service;

    @BeforeEach
    void createService() {
        userRepository = new InMemoryUserRepository();
        passwordHasher = new FakePasswordHasher();
        Clock clock = Clock.fixed(UserFixtures.CREATED_AT, ZoneOffset.UTC);
        service = new AuthenticationApplicationService(userRepository, passwordHasher, new FakeTokenIssuer(), clock);
    }

    @Test
    void signsUpAnAdministratorAndStoresOnlyTheHashOfThePassword() {
        User user = service.signUp(" Ana.Torres@Rumi.pe ", UserFixtures.FULL_NAME, UserFixtures.PASSWORD);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail().value()).isEqualTo(UserFixtures.EMAIL);
        assertThat(user.getFullName()).isEqualTo(UserFixtures.FULL_NAME);
        assertThat(user.getRole()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(user.getBuildingIds()).isEmpty();
        assertThat(user.getCreatedAt()).isEqualTo(UserFixtures.CREATED_AT);
        assertThat(user.getPasswordHash()).isEqualTo(FakePasswordHasher.PREFIX + UserFixtures.PASSWORD);
        assertThat(userRepository.all()).containsExactly(user);
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegisteredWhateverItsCase() {
        service.signUp(UserFixtures.EMAIL, UserFixtures.FULL_NAME, UserFixtures.PASSWORD);

        assertThatThrownBy(() -> service.signUp("ANA.TORRES@rumi.pe", "Another Name", "another-password"))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("An account with this email already exists.");
        assertThat(userRepository.all()).hasSize(1);
    }

    static Stream<String> passwordsThatBreakThePolicy() {
        return Stream.of(null, "short", "ñ".repeat(7), "a".repeat(73), "ñ".repeat(37));
    }

    @ParameterizedTest
    @MethodSource("passwordsThatBreakThePolicy")
    void rejectsAPasswordThatBreaksThePolicyBeforeHashingIt(String password) {
        assertThatThrownBy(() -> service.signUp(UserFixtures.EMAIL, UserFixtures.FULL_NAME, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PasswordPolicy.MESSAGE);
        assertThat(passwordHasher.hashCalls).isZero();
        assertThat(userRepository.all()).isEmpty();
    }

    @Test
    void rejectsAMalformedEmailOnSignUp() {
        assertThatThrownBy(() -> service.signUp("not-an-email", UserFixtures.FULL_NAME, UserFixtures.PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is not a valid address");
        assertThat(userRepository.all()).isEmpty();
    }

    @Test
    void signsInWithTheRightPasswordAndIssuesAToken() {
        User registered = service.signUp(UserFixtures.EMAIL, UserFixtures.FULL_NAME, UserFixtures.PASSWORD);

        AuthenticatedUser authenticated = service.signIn("Ana.Torres@rumi.pe", UserFixtures.PASSWORD);

        assertThat(authenticated.user().getId()).isEqualTo(registered.getId());
        assertThat(authenticated.token().value()).isEqualTo("token-for-" + registered.getId());
        assertThat(authenticated.token().expiresAt()).isEqualTo(FakeTokenIssuer.EXPIRES_AT);
    }

    @Test
    void rejectsAWrongPassword() {
        service.signUp(UserFixtures.EMAIL, UserFixtures.FULL_NAME, UserFixtures.PASSWORD);

        assertThatThrownBy(() -> service.signIn(UserFixtures.EMAIL, "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void rejectsAnUnknownEmailWithTheSameErrorAndStillComparesAPassword() {
        assertThatThrownBy(() -> service.signIn("nobody@rumi.pe", UserFixtures.PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
        // A hash comparison still runs, so the response time does not reveal that the email is unknown.
        assertThat(passwordHasher.comparedHashes).hasSize(1);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "not-an-email"})
    void rejectsAMissingOrMalformedEmailOnSignInAsInvalidCredentials(String email) {
        assertThatThrownBy(() -> service.signIn(email, UserFixtures.PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsAMissingPasswordOnSignIn() {
        service.signUp(UserFixtures.EMAIL, UserFixtures.FULL_NAME, UserFixtures.PASSWORD);

        assertThatThrownBy(() -> service.signIn(UserFixtures.EMAIL, null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void signInDoesNotChangeTheStoredUser() {
        User registered = service.signUp(UserFixtures.EMAIL, UserFixtures.FULL_NAME, UserFixtures.PASSWORD);

        service.signIn(UserFixtures.EMAIL, UserFixtures.PASSWORD);

        assertThat(userRepository.findByEmail(new EmailAddress(UserFixtures.EMAIL)))
                .hasValueSatisfying(stored -> assertThat(stored.getPasswordHash()).isEqualTo(registered.getPasswordHash()));
    }
}
