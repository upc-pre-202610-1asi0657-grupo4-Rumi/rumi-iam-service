package com.rumi.iam.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * An email address, trimmed and lowercased so that two spellings of the same address are equal.
 */
public record EmailAddress(String value) {

    public static final int MAX_LENGTH = 254;

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Email must have at most " + MAX_LENGTH + " characters");
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Email is not a valid address");
        }
    }
}
