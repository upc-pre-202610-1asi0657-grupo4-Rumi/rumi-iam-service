package com.rumi.iam.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {

    /**
     * Locks the row (SELECT ... FOR UPDATE) so that two resets with the same link at the same time run one after
     * the other, and the second one sees the link already used.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PasswordResetTokenJpaEntity t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int markUnusedAsUsed(@Param("userId") UUID userId, @Param("now") Instant now);
}
