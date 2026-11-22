package com.rumi.iam.infrastructure.web;

/**
 * Raised when a request that needs a signed-in user lacks a valid {@code X-User-Id} header, that is, when it did
 * not come through the API gateway with a valid token.
 */
public class MissingUserIdException extends RuntimeException {

    public static final String MESSAGE =
            "The request has no valid X-User-Id header. Call this endpoint through the API gateway with a valid token.";

    public MissingUserIdException() {
        super(MESSAGE);
    }
}
