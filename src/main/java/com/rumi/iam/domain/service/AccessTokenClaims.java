package com.rumi.iam.domain.service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Claims of a valid access token that the token reissue needs.
 *
 * @param userId   subject of the token
 * @param authTime when the user signed in with a password; null for tokens issued before the claim existed
 */
public record AccessTokenClaims(UUID userId, Instant authTime) {

    public AccessTokenClaims {
        Objects.requireNonNull(userId, "User id is required");
    }
}
