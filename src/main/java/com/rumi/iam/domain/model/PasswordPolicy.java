package com.rumi.iam.domain.model;

public final class PasswordPolicy {

    public static final String MESSAGE = "";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void requireValid(String password) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
