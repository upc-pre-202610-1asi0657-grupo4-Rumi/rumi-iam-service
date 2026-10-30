package com.rumi.iam.application;

import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.service.IssuedToken;

/**
 * Result of a successful sign-in: the user and the token issued for it.
 */
public record AuthenticatedUser(User user, IssuedToken token) {
}
