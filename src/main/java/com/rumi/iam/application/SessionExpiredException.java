package com.rumi.iam.application;

import java.time.Duration;

/**
 * The password sign-in is older than the maximum session age: the token cannot be reissued.
 */
public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException(Duration maxSessionAge) {
        super("The session started more than " + describe(maxSessionAge) + " ago; sign in again");
    }

    private static String describe(Duration duration) {
        if (duration.toMinutesPart() == 0 && duration.toSecondsPart() == 0) {
            return duration.toHours() + " hours";
        }
        return duration.toString();
    }
}
