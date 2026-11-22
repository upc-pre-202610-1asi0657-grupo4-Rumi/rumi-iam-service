package com.rumi.iam.domain.service;

/**
 * Creates the random tokens of the password reset links and the one-way hash that is stored for them.
 */
public interface ResetTokenGenerator {

    GeneratedResetToken generate();

    /**
     * The same hash that {@link #generate()} returns for a token, used to look up the token of a link.
     */
    String hash(String rawToken);
}
