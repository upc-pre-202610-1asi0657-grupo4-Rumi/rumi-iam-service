package com.rumi.iam.application;

import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.service.PasswordResetNotifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps every reset link it is asked to send, so that tests can read the raw token as the user would.
 */
public class CapturingPasswordResetNotifier implements PasswordResetNotifier {

    public record SentLink(UUID userId, String rawToken, Instant expiresAt) {
    }

    private final List<SentLink> sent = new ArrayList<>();

    @Override
    public synchronized void notify(User user, String rawToken, Instant expiresAt) {
        sent.add(new SentLink(user.getId(), rawToken, expiresAt));
    }

    public synchronized List<SentLink> sent() {
        return List.copyOf(sent);
    }

    public synchronized Optional<SentLink> last() {
        return sent.isEmpty() ? Optional.empty() : Optional.of(sent.get(sent.size() - 1));
    }

    public synchronized void clear() {
        sent.clear();
    }
}
