package com.rumi.iam.domain.service;

import com.rumi.iam.domain.model.User;

import java.time.Instant;

/**
 * Anti-Corruption Layer towards whatever delivers the password reset link to the user (US03), so that the
 * domain does not depend on a mail provider. Implementations build the link from the raw token and must never
 * write the token anywhere else.
 */
public interface PasswordResetNotifier {

    void notify(User user, String rawToken, Instant expiresAt);
}
