package com.rumi.iam.application;

import com.rumi.iam.MutableClock;
import com.rumi.iam.UserFixtures;
import com.rumi.iam.domain.service.AccessTokenClaims;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenReissueApplicationServiceTest {

    private static final Instant SIGNED_IN_AT = Instant.parse("2026-11-28T06:00:00Z");
    private static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");

    private final Map<String, AccessTokenClaims> validTokens = new HashMap<>();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final FakeTokenIssuer tokenIssuer = new FakeTokenIssuer();
    private final MutableClock clock = new MutableClock(SIGNED_IN_AT.plus(Duration.ofHours(1)));
    private final TokenReissueApplicationService service = new TokenReissueApplicationService(
            token -> Optional.ofNullable(validTokens.get(token)), tokenIssuer, users,
            new TokenReissuePolicy(Duration.ofHours(12)), clock);

    @Test
    void reissuesATokenWithTheCurrentBuildingsAndTheOriginalSignInTime() {
        users.save(UserFixtures.administrator().linkBuilding(BUILDING_ID));
        validTokens.put("current", new AccessTokenClaims(UserFixtures.USER_ID, SIGNED_IN_AT));

        AuthenticatedUser reissued = service.reissue(UserFixtures.USER_ID, "current");

        assertThat(reissued.user().getBuildingIds()).containsExactly(BUILDING_ID);
        assertThat(reissued.token().value()).isEqualTo("token-for-" + UserFixtures.USER_ID);
        assertThat(tokenIssuer.lastAuthTime).isEqualTo(SIGNED_IN_AT);
    }

    @Test
    void stillReissuesRightAtTheMaximumSessionAge() {
        users.save(UserFixtures.administrator());
        validTokens.put("current", new AccessTokenClaims(UserFixtures.USER_ID, SIGNED_IN_AT));
        clock.setInstant(SIGNED_IN_AT.plus(Duration.ofHours(12)));

        assertThat(service.reissue(UserFixtures.USER_ID, "current")).isNotNull();
    }

    @Test
    void comparesAtTheSecondPrecisionOfTheAuthTimeClaim() {
        users.save(UserFixtures.administrator());
        validTokens.put("current", new AccessTokenClaims(UserFixtures.USER_ID, SIGNED_IN_AT));
        clock.setInstant(SIGNED_IN_AT.plus(Duration.ofHours(12)).plusMillis(400));

        assertThat(service.reissue(UserFixtures.USER_ID, "current")).isNotNull();
    }

    @Test
    void refusesToReissueWhenThePasswordSignInIsOlderThanTwelveHours() {
        users.save(UserFixtures.administrator());
        validTokens.put("current", new AccessTokenClaims(UserFixtures.USER_ID, SIGNED_IN_AT));
        clock.setInstant(SIGNED_IN_AT.plus(Duration.ofHours(12)).plusSeconds(1));

        assertThatThrownBy(() -> service.reissue(UserFixtures.USER_ID, "current"))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessage("The session started more than 12 hours ago; sign in again");
    }

    @Test
    void refusesATokenWithoutAuthTime() {
        users.save(UserFixtures.administrator());
        validTokens.put("legacy", new AccessTokenClaims(UserFixtures.USER_ID, null));

        assertThatThrownBy(() -> service.reissue(UserFixtures.USER_ID, "legacy"))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void refusesAnInvalidToken() {
        users.save(UserFixtures.administrator());

        assertThatThrownBy(() -> service.reissue(UserFixtures.USER_ID, "forged"))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessage("The access token is not valid");
    }

    @Test
    void refusesTheTokenOfAnotherUser() {
        users.save(UserFixtures.administrator());
        validTokens.put("someone-else", new AccessTokenClaims(UUID.randomUUID(), SIGNED_IN_AT));

        assertThatThrownBy(() -> service.reissue(UserFixtures.USER_ID, "someone-else"))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void refusesAUserThatNoLongerExists() {
        validTokens.put("current", new AccessTokenClaims(UserFixtures.USER_ID, SIGNED_IN_AT));

        assertThatThrownBy(() -> service.reissue(UserFixtures.USER_ID, "current"))
                .isInstanceOf(InvalidAccessTokenException.class);
    }
}
