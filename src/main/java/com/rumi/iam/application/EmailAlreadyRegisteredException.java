package com.rumi.iam.application;

/**
 * Raised when signing up with an email that already belongs to a user. The message does not repeat the email.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public static final String MESSAGE = "An account with this email already exists.";

    public EmailAlreadyRegisteredException() {
        super(MESSAGE);
    }
}
