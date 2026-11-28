package com.rumi.iam.application;

/**
 * The bearer token is missing, not valid, or does not belong to the signed-in user.
 */
public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
        super("The access token is not valid");
    }
}
