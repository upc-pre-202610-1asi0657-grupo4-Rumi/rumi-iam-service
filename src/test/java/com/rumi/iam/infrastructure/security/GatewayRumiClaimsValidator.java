package com.rumi.iam.infrastructure.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Copy of {@code RumiClaimsValidator} of rumi-api-gateway (develop, infrastructure/security). If the gateway
 * changes its checks, this copy must follow, or the compatibility test stops proving anything.
 */
class GatewayRumiClaimsValidator implements OAuth2TokenValidator<Jwt> {

    private static final Set<String> GATEWAY_ROLES = Set.of("ADMINISTRATOR", "RESIDENT");

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getExpiresAt() == null) {
            return invalid("The token has no expiration");
        }
        if (!isUuid(jwt.getSubject())) {
            return invalid("The subject is not a user id");
        }
        if (!(jwt.getClaim("role") instanceof String role && GATEWAY_ROLES.contains(role))) {
            return invalid("The role is not recognized");
        }
        if (!(jwt.getClaim("buildingIds") instanceof List<?> values
                && values.stream().allMatch(GatewayRumiClaimsValidator::isUuid))) {
            return invalid("The building ids are not a list of ids");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static boolean isUuid(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        try {
            return UUID.fromString(text).toString().equalsIgnoreCase(text);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static OAuth2TokenValidatorResult invalid(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }
}
