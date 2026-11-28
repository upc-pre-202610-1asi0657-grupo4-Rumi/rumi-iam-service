package com.rumi.iam.infrastructure.persistence;

import com.rumi.iam.domain.repository.ProcessedEventRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class JpaProcessedEventRepository implements ProcessedEventRepository {

    private final SpringDataProcessedEventRepository springDataRepository;

    public JpaProcessedEventRepository(SpringDataProcessedEventRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public boolean isProcessed(UUID eventId) {
        return springDataRepository.existsById(eventId);
    }

    /**
     * Persists (never merges): the same eventId applied twice at the same time fails on the primary key, and the
     * second transaction rolls back instead of applying the event again.
     */
    @Override
    public void markProcessed(UUID eventId, String eventType, Instant processedAt) {
        springDataRepository.saveAndFlush(new ProcessedEventJpaEntity(eventId, eventType, processedAt));
    }
}
