package com.rumi.iam.domain.service;

import com.rumi.iam.domain.model.User;

/**
 * Issues the access token that the API gateway validates on every request.
 */
public interface TokenIssuer {

    IssuedToken issue(User user);
}
