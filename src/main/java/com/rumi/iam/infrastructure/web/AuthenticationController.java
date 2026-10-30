package com.rumi.iam.infrastructure.web;

import com.rumi.iam.application.AuthenticationApplicationService;
import com.rumi.iam.infrastructure.web.dto.AuthenticationResponse;
import com.rumi.iam.infrastructure.web.dto.SignInRequest;
import com.rumi.iam.infrastructure.web.dto.SignUpRequest;
import com.rumi.iam.infrastructure.web.dto.UserResponse;

public class AuthenticationController {

    public AuthenticationController(AuthenticationApplicationService authenticationService) {
    }

    public UserResponse signUp(SignUpRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public AuthenticationResponse signIn(SignInRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
