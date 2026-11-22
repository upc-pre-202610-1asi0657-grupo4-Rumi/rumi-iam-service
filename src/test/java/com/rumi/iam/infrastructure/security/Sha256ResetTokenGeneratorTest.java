package com.rumi.iam.infrastructure.security;

import com.rumi.iam.domain.service.GeneratedResetToken;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256ResetTokenGeneratorTest {

    private final Sha256ResetTokenGenerator generator = new Sha256ResetTokenGenerator();

    @Test
    void generatesThirtyTwoRandomBytesAsUrlSafeBase64WithoutPadding() {
        GeneratedResetToken token = generator.generate();

        assertThat(token.raw()).hasSize(43).matches("^[A-Za-z0-9_-]+$");
        assertThat(Base64.getUrlDecoder().decode(token.raw())).hasSize(32);
    }

    @Test
    void storesTheSha256OfTheRawTokenInLowercaseHexadecimal() {
        GeneratedResetToken token = generator.generate();

        assertThat(token.hash()).matches("^[0-9a-f]{64}$").isEqualTo(generator.hash(token.raw()));
        assertThat(token.hash()).doesNotContain(token.raw());
    }

    @Test
    void hashesWithSha256() {
        assertThat(generator.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void neverGeneratesTheSameTokenTwice() {
        Set<String> raws = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            raws.add(generator.generate().raw());
        }

        assertThat(raws).hasSize(100);
    }

    @Test
    void neverPrintsTheRawToken() {
        GeneratedResetToken token = generator.generate();

        assertThat(token.toString()).doesNotContain(token.raw());
    }
}
