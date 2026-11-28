package com.rumi.iam.domain.service;

import java.util.Optional;

/**
 * Reads an access token issued by this service. Empty when the token is not one of ours, is tampered with or has
 * expired.
 */
public interface AccessTokenReader {

    Optional<AccessTokenClaims> read(String token);
}
