package com.rumi.iam.domain.repository;

import com.rumi.iam.domain.model.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Marks every unused token of the user as used at {@code now}, so that none of them works any more.
     * Tokens already used keep their original time.
     */
    void invalidateUnusedByUserId(UUID userId, Instant now);
}
