package com.rumi.iam.application;

import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.service.IssuedToken;
import com.rumi.iam.domain.service.TokenIssuer;

import java.time.Instant;

class FakeTokenIssuer implements TokenIssuer {

    static final Instant EXPIRES_AT = Instant.parse("2026-10-29T16:00:00Z");

    @Override
    public IssuedToken issue(User user) {
        return new IssuedToken("token-for-" + user.getId(), EXPIRES_AT);
    }
}
