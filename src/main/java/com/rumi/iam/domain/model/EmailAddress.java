package com.rumi.iam.domain.model;

public record EmailAddress(String value) {

    public static final int MAX_LENGTH = 254;

    public EmailAddress {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
