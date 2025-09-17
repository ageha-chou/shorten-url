package com.diepnn.shortenurl.utils;

import com.diepnn.shortenurl.common.enums.PersistableEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnumUtilsTests {
    // Sample enum for testing
    enum SampleStatus implements PersistableEnum {
        ACTIVE("A"),
        INACTIVE("I");

        private final String value;

        SampleStatus(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    @Test
    void fromValue_returnsMatchingEnum() {
        SampleStatus result = EnumUtils.fromValue(SampleStatus.class, "A");
        assertEquals(SampleStatus.ACTIVE, result);

        result = EnumUtils.fromValue(SampleStatus.class, "I");
        assertEquals(SampleStatus.INACTIVE, result);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    void fromValue_returnsNullForBlank(String input) {
        SampleStatus result = EnumUtils.fromValue(SampleStatus.class, input);
        assertNull(result);
    }

    @Test
    void fromValue_throwsForUnknownValue() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> EnumUtils.fromValue(SampleStatus.class, "X")
                                                  );
        assertTrue(ex.getMessage().contains("Unknown value"));
        assertTrue(ex.getMessage().contains("SampleStatus"));
    }

    @Test
    void fromValue_isCaseInsensitiveByDesign() {
        SampleStatus result = EnumUtils.fromValue(SampleStatus.class, "a");
        assertEquals(SampleStatus.ACTIVE, result);
    }
}
