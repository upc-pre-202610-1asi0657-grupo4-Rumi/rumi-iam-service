package com.rumi.iam.application;

import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.repository.UserRepository;
import com.rumi.iam.domain.service.PasswordHasher;
import com.rumi.iam.domain.service.TokenIssuer;

import java.time.Clock;

public class AuthenticationApplicationService {

    public AuthenticationApplicationService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer,
            Clock clock
    ) {
    }

    public User signUp(String email, String fullName, String password) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public AuthenticatedUser signIn(String email, String password) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
