package com.rumi.iam.infrastructure.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the id of the signed-in user, taken from the {@code X-User-Id} header that rumi-api-gateway sets from the
 * {@code sub} claim of a valid token. The gateway removes any such header sent by the client, so IAM must only be
 * reachable through the gateway. A missing or malformed header gives 401.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {

    String HEADER = "X-User-Id";
}
