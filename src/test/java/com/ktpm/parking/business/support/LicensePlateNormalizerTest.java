package com.ktpm.parking.business.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicensePlateNormalizerTest {

    @Test
    void shouldRemoveWhitespaceAndUppercaseLicensePlate() {
        assertThat(LicensePlateNormalizer.normalize(" 29a - 12345 "))
                .isEqualTo("29A-12345");
    }

    @Test
    void shouldRejectBlankLicensePlate() {
        assertThatThrownBy(() -> LicensePlateNormalizer.normalize("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
