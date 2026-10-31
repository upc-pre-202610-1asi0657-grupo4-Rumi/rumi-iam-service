package com.rumi.iam.domain.model;

/**
 * Roles carried in the {@code role} claim of the tokens, as rumi-api-gateway expects them.
 */
public enum Role {
    ADMINISTRATOR,
    RESIDENT
}
