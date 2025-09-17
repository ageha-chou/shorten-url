package com.diepnn.shortenurl.common.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShortenUrlBase62GeneratorTests {
    private final ShortenUrlGenerator<Long, String> generator = new ShortenUrlBase62Generator();

    @Test
    @DisplayName("throw IllegalArgumentException when original is null or less than or equals to 0")
    public void generate_originalIsNullOrLessThan0_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(-1L));
    }

    @Test
    @DisplayName("return 0 when original equals to 0")
    public void generate_originalEqualsTo0_return0() {
        String result = generator.generate(0L);
        assertEquals("0", result, "Wrong result");
    }

    @Test
    @DisplayName("return correct result when original is greater than 0")
    public void generate_originalIsGreaterThan0_returnCorrectResult() {
        String result = generator.generate(125L);
        assertEquals("21", result, "Wrong result");

    }
}
