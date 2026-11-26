package com.yusuf.jwt_demo.violation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

// Saf birim testi: Spring ve DB yok, milisaniyede çalışır.
class PlateNormalizationTests {

    @ParameterizedTest(name = "[{0}] → {1}")
    @CsvSource(delimiter = '|', value = {
            "34A0261       | 34A0261",
            "34a0261       | 34A0261",
            "' 34 a 0261 ' | 34A0261",
            "'34\tA\t0261' | 34A0261",
            "06abc12       | 06ABC12",
    })
    void normalizes(String input, String expected) {
        assertThat(Violation.normalizePlate(input)).isEqualTo(expected);
    }

    @Test
    void nullStaysNull() {
        assertThat(Violation.normalizePlate(null)).isNull();
    }

    @Test
    void turkishLocaleDoesNotProduceDottedCapitalI() {
        // Locale.ROOT kullanılmasaydı Türkçe locale'de "i" → "İ" olurdu ve sahiplik karşılaştırması bozulurdu
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(Violation.normalizePlate("34ib01")).isEqualTo("34IB01");
        } finally {
            Locale.setDefault(original);
        }
    }
}
