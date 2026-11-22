package com.rumi.iam.application;

/**
 * Raised for an unknown, expired or already used reset link alike, so that a client cannot tell them apart.
 * The message never contains the token.
 */
public class InvalidResetTokenException extends RuntimeException {

    public static final String MESSAGE = "Invalid or expired token.";

    public InvalidResetTokenException() {
        super(MESSAGE);
    }
}
