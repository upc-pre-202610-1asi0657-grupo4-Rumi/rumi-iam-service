package com.rumi.iam.domain.model;

import java.nio.charset.StandardCharsets;

/**
 * Length rule of a plain-text password. The minimum is a strength rule, counted in characters (code points).
 * The maximum is counted in UTF-8 bytes because BCrypt ignores everything after the first 72 bytes.
 */
public final class PasswordPolicy {

    public static final int MIN_CHARACTERS = 8;
    public static final int MAX_BYTES = 72;
    public static final String MESSAGE =
            "Password must have at least " + MIN_CHARACTERS + " characters and at most " + MAX_BYTES + " bytes";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null
                && password.codePointCount(0, password.length()) >= MIN_CHARACTERS
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

    /**
     * Fails with a message that describes the rule and never contains the password.
     */
    public static void requireValid(String password) {
        if (!isValid(password)) {
            throw new IllegalArgumentException(MESSAGE);
        }
    }
}
