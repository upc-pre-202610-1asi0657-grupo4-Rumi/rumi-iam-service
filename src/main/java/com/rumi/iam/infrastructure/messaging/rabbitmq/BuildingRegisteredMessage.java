package com.rumi.iam.infrastructure.messaging.rabbitmq;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON published by rumi-building-service on rumi.building-management.events with routing key
 * building-management.building.registered. Unknown fields are ignored, so the publisher can add fields.
 */
public record BuildingRegisteredMessage(UUID eventId, UUID buildingId, UUID administratorId, Instant occurredAt) {

    boolean isComplete() {
        return eventId != null && buildingId != null && administratorId != null && occurredAt != null;
    }
}
