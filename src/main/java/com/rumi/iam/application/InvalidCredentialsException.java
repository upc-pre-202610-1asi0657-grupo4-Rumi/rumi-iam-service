package com.rumi.iam.application;

/**
 * Raised for an unknown email and for a wrong password alike, so that a client cannot tell which emails exist.
 */
public class InvalidCredentialsException extends RuntimeException {

    public static final String MESSAGE = "Invalid email or password.";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }
}
