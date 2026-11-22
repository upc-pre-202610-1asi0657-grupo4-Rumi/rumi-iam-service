package com.rumi.iam.application;

import com.rumi.iam.domain.model.PasswordResetToken;
import com.rumi.iam.domain.repository.PasswordResetTokenRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final Map<UUID, PasswordResetToken> tokens = new LinkedHashMap<>();

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        tokens.put(token.getId(), token);
        return token;
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return tokens.values().stream().filter(token -> token.getTokenHash().equals(tokenHash)).findFirst();
    }

    @Override
    public void invalidateUnusedByUserId(UUID userId, Instant now) {
        tokens.replaceAll((id, token) -> token.getUserId().equals(userId) && token.getUsedAt() == null
                ? token.markUsed(now)
                : token);
    }

    List<PasswordResetToken> all() {
        return List.copyOf(tokens.values());
    }
}
