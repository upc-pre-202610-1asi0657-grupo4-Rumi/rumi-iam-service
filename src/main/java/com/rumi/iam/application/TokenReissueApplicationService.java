package com.rumi.iam.application;

import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.repository.UserRepository;
import com.rumi.iam.domain.service.AccessTokenClaims;
import com.rumi.iam.domain.service.AccessTokenReader;
import com.rumi.iam.domain.service.TokenIssuer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Issues a new token for the signed-in user with the buildings IAM knows now, for example right after the user
 * registered a building. The new token keeps the auth_time of the password sign-in, so a session cannot be
 * stretched forever: after the maximum session age the user must sign in again. Revocation (tokenVersion) is a
 * separate, later change.
 */
@Service
@Transactional(readOnly = true)
public class TokenReissueApplicationService {

    private final AccessTokenReader tokenReader;
    private final TokenIssuer tokenIssuer;
    private final UserRepository userRepository;
    private final TokenReissuePolicy policy;
    private final Clock clock;

    public TokenReissueApplicationService(AccessTokenReader tokenReader, TokenIssuer tokenIssuer,
                                          UserRepository userRepository, TokenReissuePolicy policy, Clock clock) {
        this.tokenReader = tokenReader;
        this.tokenIssuer = tokenIssuer;
        this.userRepository = userRepository;
        this.policy = policy;
        this.clock = clock;
    }

    /**
     * @param requesterId the user forwarded by the gateway in X-User-Id
     * @param accessToken the bearer token of the request
     * @throws InvalidAccessTokenException if the token is not valid, belongs to someone else or the user is gone
     * @throws SessionExpiredException     if the password sign-in is older than the maximum session age
     */
    public AuthenticatedUser reissue(UUID requesterId, String accessToken) {
        AccessTokenClaims claims = tokenReader.read(accessToken).orElseThrow(InvalidAccessTokenException::new);
        if (!claims.userId().equals(requesterId)) {
            throw new InvalidAccessTokenException();
        }
        Instant authTime = claims.authTime();
        // auth_time is whole seconds in the token, so the comparison is too.
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        if (authTime == null || authTime.plus(policy.maxSessionAge()).isBefore(now)) {
            throw new SessionExpiredException(policy.maxSessionAge());
        }
        User user = userRepository.findById(requesterId).orElseThrow(InvalidAccessTokenException::new);
        return new AuthenticatedUser(user, tokenIssuer.issue(user, authTime));
    }
}
