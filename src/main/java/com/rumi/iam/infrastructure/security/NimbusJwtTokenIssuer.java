package com.rumi.iam.infrastructure.security;

import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.service.IssuedToken;
import com.rumi.iam.domain.service.TokenIssuer;

import java.time.Clock;

public class NimbusJwtTokenIssuer implements TokenIssuer {

    public NimbusJwtTokenIssuer(JwtSecurityProperties properties, Clock clock) {
    }

    @Override
    public IssuedToken issue(User user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
