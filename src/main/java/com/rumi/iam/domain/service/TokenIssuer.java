package com.rumi.iam.domain.service;

import com.rumi.iam.domain.model.User;

import java.time.Instant;

/**
 * Issues the access token that the API gateway validates on every request.
 */
public interface TokenIssuer {

    /**
     * @param authTime when the user signed in with a password; a reissued token keeps the original one
     */
    IssuedToken issue(User user, Instant authTime);
}
