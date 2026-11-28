package com.rumi.iam.infrastructure.security;

/**
 * Custom claims of the issued tokens, with the names rumi-api-gateway reads. {@code sub} holds the user id.
 */
public final class JwtClaimNames {

    public static final String ROLE = "role";
    public static final String BUILDING_IDS = "buildingIds";
    /** When the user signed in with a password (epoch seconds, as in OpenID Connect). Kept by a reissue. */
    public static final String AUTH_TIME = "auth_time";

    private JwtClaimNames() {
    }
}
