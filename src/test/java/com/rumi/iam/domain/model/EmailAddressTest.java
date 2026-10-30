package com.rumi.iam.domain.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("Enabled by the next feat commit")
class EmailAddressTest {

    @Test
    void trimsAndLowercasesTheAddress() {
        assertThat(new EmailAddress("  Ana.Torres@Rumi.PE ").value()).isEqualTo("ana.torres@rumi.pe");
    }

    @Test
    void twoAddressesThatOnlyDifferInCaseAreEqual() {
        assertThat(new EmailAddress("ANA@rumi.pe")).isEqualTo(new EmailAddress("ana@rumi.pe"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsAMissingAddress(String value) {
        assertThatThrownBy(() -> new EmailAddress(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana", "ana@", "@rumi.pe", "ana@rumi", "ana torres@rumi.pe", "ana@@rumi.pe"})
    void rejectsAMalformedAddress(String value) {
        assertThatThrownBy(() -> new EmailAddress(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is not a valid address");
    }

    @Test
    void rejectsAnAddressLongerThanTheMaximum() {
        String local = "a".repeat(EmailAddress.MAX_LENGTH - "@rumi.pe".length() + 1);

        assertThatThrownBy(() -> new EmailAddress(local + "@rumi.pe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email must have at most 254 characters");
    }

    @Test
    void acceptsAnAddressOfExactlyTheMaximumLength() {
        String local = "a".repeat(EmailAddress.MAX_LENGTH - "@rumi.pe".length());

        assertThat(new EmailAddress(local + "@rumi.pe").value()).hasSize(EmailAddress.MAX_LENGTH);
    }
}
