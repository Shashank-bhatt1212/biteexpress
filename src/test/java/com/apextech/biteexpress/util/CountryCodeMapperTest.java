package com.apextech.biteexpress.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CountryCodeMapperTest {

    @ParameterizedTest(name = "code {0} -> {1}")
    @CsvSource({
            "1, India",
            "14, Australia",
            "30, Brazil",
            "37, Canada",
            "94, Indonesia",
            "148, New Zealand",
            "162, Philippines",
            "166, Qatar",
            "184, Singapore",
            "189, South Africa",
            "191, Sri Lanka",
            "208, Turkey",
            "214, UAE",
            "215, United Kingdom",
            "216, United States"
    })
    @DisplayName("resolves every known country code")
    void resolvesKnownCodes(int code, String expected) {
        assertThat(CountryCodeMapper.resolve(code)).isEqualTo(expected);
    }

    @Test
    @DisplayName("unknown code resolves to Unknown")
    void unknownCode() {
        assertThat(CountryCodeMapper.resolve(999)).isEqualTo("Unknown");
    }
}
