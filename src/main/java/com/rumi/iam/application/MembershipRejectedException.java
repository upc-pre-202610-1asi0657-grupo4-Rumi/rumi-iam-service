package com.rumi.iam.application;

/**
 * A membership event cannot be applied: retrying it will not help, so it goes to the dead letter queue.
 */
public class MembershipRejectedException extends RuntimeException {

    public MembershipRejectedException(String message) {
        super(message);
    }
}
