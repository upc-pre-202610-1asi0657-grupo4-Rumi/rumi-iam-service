package com.rumi.iam.application;

import com.rumi.iam.domain.repository.ProcessedEventRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class InMemoryProcessedEventRepository implements ProcessedEventRepository {

    final Map<UUID, String> processed = new HashMap<>();

    @Override
    public boolean isProcessed(UUID eventId) {
        return processed.containsKey(eventId);
    }

    @Override
    public void markProcessed(UUID eventId, String eventType, Instant processedAt) {
        if (processed.putIfAbsent(eventId, eventType) != null) {
            throw new IllegalStateException("Event " + eventId + " already processed");
        }
    }
}
