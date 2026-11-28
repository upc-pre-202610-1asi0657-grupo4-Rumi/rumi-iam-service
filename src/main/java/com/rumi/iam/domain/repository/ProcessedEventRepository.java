package com.rumi.iam.domain.repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Integration events already applied, by eventId. Events are delivered at least once; a redelivered event is
 * recognised here and discarded.
 */
public interface ProcessedEventRepository {

    boolean isProcessed(UUID eventId);

    void markProcessed(UUID eventId, String eventType, Instant processedAt);
}
