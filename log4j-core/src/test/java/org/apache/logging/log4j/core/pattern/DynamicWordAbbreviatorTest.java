package org.apache.logging.log4j.core.pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the {@link DynamicWordAbbreviator} class.
 */
class DynamicWordAbbreviatorTest extends Assertions {

    @Test
    void testNullAndEmptyInputs() {
        DynamicWordAbbreviator abbreviator = DynamicWordAbbreviator.create("1.1*");

        assertDoesNotThrow(() -> abbreviator.abbreviate("orig", null));
        assertDoesNotThrow(() -> abbreviator.abbreviate(null, new StringBuilder()));

        StringBuilder dest = new StringBuilder();
        abbreviator.abbreviate(null, dest);
        assertEquals("", dest.toString());

        abbreviator.abbreviate("", dest);
        assertEquals("", dest.toString());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
            "",
            " ",
            "0.0*",
            "0,0*",
            "1.2",
            "1.2**",
            "1.0*"
    })
    void testInvalidPatterns(String pattern) {
        assertNull(DynamicWordAbbreviator.create(pattern));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" \"{1}\" \"{2}\"")
    @CsvSource(delimiter = '|', value = {
            "1.1*|.|.",
            "1.1*|\\ |\\ ",
            "1.1*|org.novice.o|o.n.o",
            "1.1*|org.novice.|o.novice",
            "1.1*|org......novice|o.novice",
            "1.1*|org. . .novice|o. . .novice",
    })
    void testStrangeWords(String pattern, String input, String expected) {
        DynamicWordAbbreviator abbreviator = DynamicWordAbbreviator.create(pattern);
        StringBuilder actual = new StringBuilder();
        abbreviator.abbreviate(input, actual);
        assertEquals(expected, actual.toString());
    }

}

