package com.rumi.iam.infrastructure.security;

/**
 * Custom claims of the issued tokens, with the names rumi-api-gateway reads. {@code sub} holds the user id.
 */
public final class JwtClaimNames {

    public static final String ROLE = "role";
    public static final String BUILDING_IDS = "buildingIds";

    private JwtClaimNames() {
    }
}
