package com.rumi.iam.infrastructure.notification;

import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.service.PasswordResetNotifier;
import com.rumi.iam.infrastructure.security.PasswordResetProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * DEVELOPMENT STAND-IN. Rumi has no mail sender yet, so the reset link is written to the log instead of being
 * e-mailed. This log line is the only place where the link appears. Replace this adapter with a mail adapter
 * before real users depend on password recovery.
 */
@Component
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    private final PasswordResetProperties properties;

    public LoggingPasswordResetNotifier(PasswordResetProperties properties) {
        this.properties = properties;
    }

    @Override
    public void notify(User user, String rawToken, Instant expiresAt) {
        log.warn("Password reset link (development stand-in, no e-mail is sent) for user {}, valid until {}: {}",
                user.getId(), expiresAt, link(properties.baseUrl(), rawToken));
    }

    static String link(String baseUrl, String rawToken) {
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "token=" + rawToken;
    }
}
