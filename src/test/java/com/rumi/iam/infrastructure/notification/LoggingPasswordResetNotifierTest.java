package com.rumi.iam.infrastructure.notification;

import com.rumi.iam.UserFixtures;
import com.rumi.iam.infrastructure.security.PasswordResetProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LoggingPasswordResetNotifierTest {

    private static final String RAW_TOKEN = "Qm9ndXMtcmF3LXRva2VuLWZvci10ZXN0cy1vbmx5LTEyMw";
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-29T15:30:00Z");

    @Test
    void logsTheLinkAsADevelopmentStandInWithoutTheEmail(CapturedOutput output) {
        LoggingPasswordResetNotifier notifier = new LoggingPasswordResetNotifier(
                new PasswordResetProperties("http://localhost:4200/reset-password", null));

        notifier.notify(UserFixtures.administrator(), RAW_TOKEN, EXPIRES_AT);

        assertThat(output.getOut())
                .contains("http://localhost:4200/reset-password?token=" + RAW_TOKEN)
                .contains("development stand-in")
                .contains(UserFixtures.USER_ID.toString())
                .contains(EXPIRES_AT.toString())
                .doesNotContain(UserFixtures.EMAIL);
    }

    @Test
    void buildsTheLink() {
        assertThat(LoggingPasswordResetNotifier.link("https://app.rumi.pe/reset", RAW_TOKEN))
                .isEqualTo("https://app.rumi.pe/reset?token=" + RAW_TOKEN);
        assertThat(LoggingPasswordResetNotifier.link("https://app.rumi.pe/reset?lang=es", RAW_TOKEN))
                .isEqualTo("https://app.rumi.pe/reset?lang=es&token=" + RAW_TOKEN);
    }
}
