package com.rumi.iam.infrastructure.persistence;

import com.rumi.iam.domain.model.PasswordResetToken;
import com.rumi.iam.domain.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final SpringDataPasswordResetTokenRepository springDataRepository;

    public JpaPasswordResetTokenRepository(SpringDataPasswordResetTokenRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return toDomain(springDataRepository.saveAndFlush(toEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return springDataRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void invalidateUnusedByUserId(UUID userId, Instant now) {
        springDataRepository.markUnusedAsUsed(userId, now);
    }

    private PasswordResetTokenJpaEntity toEntity(PasswordResetToken token) {
        return new PasswordResetTokenJpaEntity(
                token.getId(),
                token.getUserId(),
                token.getTokenHash(),
                token.getExpiresAt(),
                token.getUsedAt(),
                token.getCreatedAt()
        );
    }

    private PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        return new PasswordResetToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt()
        );
    }
}
