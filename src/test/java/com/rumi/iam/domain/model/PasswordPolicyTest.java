package com.rumi.iam.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The minimum is a strength rule counted in characters (code points). The maximum is counted in UTF-8
 * bytes because BCrypt only uses the first 72 bytes of a password.
 */
class PasswordPolicyTest {

    @Test
    void acceptsTheMinimumOfEightAsciiBytes() {
        assertThat(PasswordPolicy.isValid("a".repeat(8))).isTrue();
    }

    @Test
    void rejectsSevenAsciiBytes() {
        assertThat(PasswordPolicy.isValid("a".repeat(7))).isFalse();
    }

    @Test
    void acceptsTheMaximumOfSeventyTwoAsciiBytes() {
        assertThat(PasswordPolicy.isValid("a".repeat(72))).isTrue();
    }

    @Test
    void rejectsSeventyThreeAsciiBytes() {
        assertThat(PasswordPolicy.isValid("a".repeat(73))).isFalse();
    }

    @Test
    void countsMultibyteCharactersInBytes() {
        // "ñ" is two bytes in UTF-8: 24 characters are 48 bytes, 37 characters are 74 bytes.
        assertThat(PasswordPolicy.isValid("ñ".repeat(24))).isTrue();
        assertThat(PasswordPolicy.isValid("ñ".repeat(37))).isFalse();
    }

    @Test
    void rejectsFourMultibyteCharactersEvenThoughTheyAreEightBytes() {
        // The minimum is a strength rule, counted in characters: four "ñ" are eight bytes but only four characters.
        assertThat(PasswordPolicy.isValid("ñ".repeat(4))).isFalse();
        assertThat(PasswordPolicy.isValid("ñ".repeat(8))).isTrue();
    }

    @Test
    void countsTheMinimumInCodePointsNotJavaChars() {
        // Each emoji is one code point but two Java chars and four UTF-8 bytes.
        String sevenEmoji = "🌋".repeat(7);
        String eightEmoji = "🌋".repeat(8);

        assertThat(PasswordPolicy.isValid(sevenEmoji)).isFalse();
        assertThat(PasswordPolicy.isValid(eightEmoji)).isTrue();
    }

    @Test
    void appliesTheByteMaximumToMultibyteCharacters() {
        // Nineteen emoji are 76 bytes: too long for BCrypt although they are only nineteen characters.
        assertThat(PasswordPolicy.isValid("🌋".repeat(18))).isTrue();
        assertThat(PasswordPolicy.isValid("🌋".repeat(19))).isFalse();
    }

    @Test
    void rejectsAMissingPassword() {
        assertThat(PasswordPolicy.isValid(null)).isFalse();
    }

    @Test
    void requireValidDescribesTheRuleWithoutRevealingThePassword() {
        assertThatCode(() -> PasswordPolicy.requireValid("a".repeat(8))).doesNotThrowAnyException();
        assertThatThrownBy(() -> PasswordPolicy.requireValid("secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PasswordPolicy.MESSAGE)
                .message().doesNotContain("secret");
    }
}
