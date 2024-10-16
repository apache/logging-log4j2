/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.impl;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.pattern.AnsiEscape;
import org.apache.logging.log4j.core.pattern.JAnsiTextRenderer;
import org.apache.logging.log4j.core.pattern.TextRenderer;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@code ThrowableFormatOptions}.
 */
final class ThrowableFormatOptionsTest {

    /**
     * Runs a given test comparing against the expected values.
     *
     * @param options
     *            The list of options to parse.
     * @param expectedLines
     *            The expected lines.
     * @param expectedPackages
     *            The expected package filters.
     * @param expectedSeparator
     *            The expected separator.
     */
    private static ThrowableFormatOptions test(
            final String[] options,
            final int expectedLines,
            final String expectedSeparator,
            final List<String> expectedPackages) {
        final ThrowableFormatOptions tfo = ThrowableFormatOptions.newInstance(options);
        assertEquals(expectedLines, tfo.getLines(), "getLines");
        assertEquals(expectedSeparator, tfo.getSeparator(), "getSeparator");
        assertEquals(expectedPackages, tfo.getIgnorePackages(), "getPackages");
        assertEquals(expectedLines == Integer.MAX_VALUE, tfo.allLines(), "allLines");
        assertEquals(expectedLines != 0, tfo.anyLines(), "anyLines");
        assertEquals(0, tfo.minLines(0), "minLines");
        assertEquals(expectedLines, tfo.minLines(Integer.MAX_VALUE), "minLines");
        assertEquals(expectedPackages != null && !expectedPackages.isEmpty(), tfo.hasPackages(), "hasPackages");
        assertNotNull(tfo.toString(), "toString");
        return tfo;
    }

    /**
     * Test {@code %throwable} with null options.
     */
    @Test
    void testNull() {
        test(null, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable}
     */
    @Test
    void testEmpty() {
        test(new String[] {}, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{} } with null option value.
     */
    @Test
    void testOneNullElement() {
        test(new String[] {null}, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{} }
     */
    @Test
    void testOneEmptyElement() {
        test(new String[] {""}, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{full} }
     */
    @Test
    void testFull() {
        test(new String[] {"full"}, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{full}{ansi} }
     */
    @Test
    void testFullAnsi() {
        final ThrowableFormatOptions tfo =
                test(new String[] {"full", "ansi"}, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
        testFullAnsiEmptyConfig(tfo);
    }

    /**
     * Test {@code %throwable{full}{ansi()} }
     */
    @Test
    void testFullAnsiEmptyConfig() {
        final ThrowableFormatOptions tfo =
                test(new String[] {"full", "ansi()"}, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
        testFullAnsiEmptyConfig(tfo);
    }

    private void testFullAnsiEmptyConfig(final ThrowableFormatOptions tfo) {
        final TextRenderer textRenderer = tfo.getTextRenderer();
        assertNotNull(textRenderer);
        assertInstanceOf(JAnsiTextRenderer.class, textRenderer);
        final JAnsiTextRenderer ansiRenderer = (JAnsiTextRenderer) textRenderer;
        final Map<String, String> styleMap = ansiRenderer.getStyleMap();
        // We have defaults
        assertFalse(styleMap.isEmpty());
        assertNotNull(styleMap.get(toRootUpperCase("Name")));
    }

    /**
     * Test {@code %throwable{full}{ansi(Warning=red))} }
     */
    @Test
    void testFullAnsiWithCustomStyle() {
        final ThrowableFormatOptions tfo =
                test(new String[] {"full", "ansi(Warning=red)"}, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
        final TextRenderer textRenderer = tfo.getTextRenderer();
        assertNotNull(textRenderer);
        assertInstanceOf(JAnsiTextRenderer.class, textRenderer);
        final JAnsiTextRenderer ansiRenderer = (JAnsiTextRenderer) textRenderer;
        final Map<String, String> styleMap = ansiRenderer.getStyleMap();
        assertThat(styleMap.get(toRootUpperCase("Warning"))).isEqualTo(AnsiEscape.createSequence("RED"));
    }

    /**
     * Test {@code %throwable{full}{ansi(Warning=red Key=blue Value=cyan))} }
     */
    @Test
    void testFullAnsiWithCustomStyles() {
        final ThrowableFormatOptions tfo = test(
                new String[] {"full", "ansi(Warning=red Key=blue Value=cyan)"},
                Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR,
                null);
        final TextRenderer textRenderer = tfo.getTextRenderer();
        assertNotNull(textRenderer);
        assertInstanceOf(JAnsiTextRenderer.class, textRenderer);
        final JAnsiTextRenderer ansiRenderer = (JAnsiTextRenderer) textRenderer;
        final Map<String, String> styleMap = ansiRenderer.getStyleMap();
        assertThat(styleMap.get(toRootUpperCase("Warning"))).isEqualTo(AnsiEscape.createSequence("RED"));
        assertThat(styleMap.get(toRootUpperCase("Key"))).isEqualTo(AnsiEscape.createSequence("BLUE"));
        assertThat(styleMap.get(toRootUpperCase("Value"))).isEqualTo(AnsiEscape.createSequence("CYAN"));
    }

    /**
     * Test {@code %throwable{full}{ansi(Warning=red Key=blue,bg_red Value=cyan,bg_black,underline)} }
     */
    @Test
    void testFullAnsiWithCustomComplexStyles() {
        final ThrowableFormatOptions tfo = test(
                new String[] {"full", "ansi(Warning=red Key=blue,bg_red Value=cyan,bg_black,underline)"},
                Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR,
                null);
        final TextRenderer textRenderer = tfo.getTextRenderer();
        assertNotNull(textRenderer);
        assertInstanceOf(JAnsiTextRenderer.class, textRenderer);
        final JAnsiTextRenderer ansiRenderer = (JAnsiTextRenderer) textRenderer;
        final Map<String, String> styleMap = ansiRenderer.getStyleMap();
        assertThat(styleMap.get(toRootUpperCase("Warning"))).isEqualTo(AnsiEscape.createSequence("RED"));
        assertThat(styleMap.get(toRootUpperCase("Key"))).isEqualTo(AnsiEscape.createSequence("BLUE", "BG_RED"));
        assertThat(styleMap.get(toRootUpperCase("Value")))
                .isEqualTo(AnsiEscape.createSequence("CYAN", "BG_BLACK", "UNDERLINE"));
    }

    /**
     * Test {@code %throwable{none} }
     */
    @Test
    void testNone() {
        test(new String[] {"none"}, 0, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{short} }
     */
    @Test
    void testShort() {
        test(new String[] {"short"}, 2, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{10} }
     */
    @Test
    void testDepth() {
        test(new String[] {"10"}, 10, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{separator(|)} }
     */
    @Test
    void testSeparator() {
        test(new String[] {"separator(|)"}, Integer.MAX_VALUE, "|", null);
    }

    /**
     * Test {@code %throwable{separator()} }
     */
    @Test
    void testSeparatorAsEmpty() {
        test(new String[] {"separator()"}, Integer.MAX_VALUE, Strings.EMPTY, null);
    }

    /**
     * Test {@code %throwable{separator(\n)} }
     */
    @Test
    void testSeparatorAsDefaultLineSeparator() {
        test(
                new String[] {"separator(" + Strings.LINE_SEPARATOR + ')'},
                Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR,
                null);
    }

    /**
     * Test {@code %throwable{separator( | )} }
     */
    @Test
    void testSeparatorAsMultipleCharacters() {
        test(new String[] {"separator( | )"}, Integer.MAX_VALUE, " | ", null);
    }

    /**
     * Test {@code %throwable{full}{separator(|)} }
     */
    @Test
    void testFullAndSeparator() {
        test(new String[] {"full", "separator(|)"}, Integer.MAX_VALUE, "|", null);
    }

    /**
     * Test {@code %throwable{full}{filters(org.junit)}{separator(|)} }
     */
    @Test
    void testFullAndFiltersAndSeparator() {
        test(
                new String[] {"full", "filters(org.junit)", "separator(|)"},
                Integer.MAX_VALUE,
                "|",
                Collections.singletonList("org.junit"));
    }

    /**
     * Test {@code %throwable{none}{separator(|)} }
     */
    @Test
    void testNoneAndSeparator() {
        test(new String[] {"none", "separator(|)"}, 0, "|", null);
    }

    /**
     * Test {@code %throwable{short}{separator(|)} }
     */
    @Test
    void testShortAndSeparator() {
        test(new String[] {"short", "separator(|)"}, 2, "|", null);
    }

    /**
     * Test {@code %throwable{10}{separator(|)} }
     */
    @Test
    void testDepthAndSeparator() {
        test(new String[] {"10", "separator(|)"}, 10, "|", null);
    }

    /**
     * Test {@code %throwable{filters(packages)} }
     */
    @Test
    void testFilters() {
        test(
                new String[] {"filters(packages)"},
                Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR,
                Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{filters()} }
     */
    @Test
    void testFiltersAsEmpty() {
        test(new String[] {"filters()"}, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{filters(package1,package2)} }
     */
    @Test
    void testFiltersAsMultiplePackages() {
        test(
                new String[] {"filters(package1,package2)"},
                Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }

    /**
     * Test {@code %throwable{full}{filters(packages)} }
     */
    @Test
    void testFullAndFilters() {
        test(
                new String[] {"full", "filters(packages)"},
                Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR,
                Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{none}{filters(packages)} }
     */
    @Test
    void testNoneAndFilters() {
        test(
                new String[] {"none", "filters(packages)"},
                0,
                Strings.LINE_SEPARATOR,
                Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{short}{filters(packages)} }
     */
    @Test
    void testShortAndFilters() {
        test(
                new String[] {"short", "filters(packages)"},
                2,
                Strings.LINE_SEPARATOR,
                Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{10}{filters(packages)} }
     */
    @Test
    void testDepthAndFilters() {
        test(
                new String[] {"10", "filters(packages)"},
                10,
                Strings.LINE_SEPARATOR,
                Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{full}{separator(|)}{filters(packages)} }
     */
    @Test
    void testFullAndSeparatorAndFilter() {
        test(
                new String[] {"full", "separator(|)", "filters(packages)"},
                Integer.MAX_VALUE,
                "|",
                Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{full}{separator(|)}{filters(package1,package2)} }
     */
    @Test
    void testFullAndSeparatorAndFilters() {
        test(
                new String[] {"full", "separator(|)", "filters(package1,package2)"},
                Integer.MAX_VALUE,
                "|",
                Arrays.asList("package1", "package2"));
    }

    /**
     * Test {@code %throwable{none}{separator(|)}{filters(packages)} }
     */
    @Test
    void testNoneAndSeparatorAndFilters() {
        test(new String[] {"none", "separator(|)", "filters(packages)"}, 0, "|", Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{short}{separator(|)}{filters(packages)} }
     */
    @Test
    void testShortAndSeparatorAndFilters() {
        test(
                new String[] {"short", "separator(|)", "filters(packages)"},
                2,
                "|",
                Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{10}{separator(|)}{filters(packages)} }
     */
    @Test
    void testDepthAndSeparatorAndFilters() {
        test(new String[] {"10", "separator(|)", "filters(packages)"}, 10, "|", Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{full,filters(packages)} }
     */
    @Test
    void testSingleOptionFullAndFilters() {
        test(
                new String[] {"full,filters(packages)"},
                Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR,
                Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{none,filters(packages)} }
     */
    @Test
    void testSingleOptionNoneAndFilters() {
        test(new String[] {"none,filters(packages)"}, 0, Strings.LINE_SEPARATOR, Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{short,filters(packages)} }
     */
    @Test
    void testSingleOptionShortAndFilters() {
        test(
                new String[] {"short,filters(packages)"},
                2,
                Strings.LINE_SEPARATOR,
                Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{none,filters(packages)} }
     */
    @Test
    void testSingleOptionDepthAndFilters() {
        test(new String[] {"10,filters(packages)"}, 10, Strings.LINE_SEPARATOR, Collections.singletonList("packages"));
    }

    /**
     * Test {@code %throwable{full,filters(package1,package2)} }
     */
    @Test
    void testSingleOptionFullAndMultipleFilters() {
        test(
                new String[] {"full,filters(package1,package2)"},
                Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }

    /**
     * Test {@code %throwable{none,filters(package1,package2)} }
     */
    @Test
    void testSingleOptionNoneAndMultipleFilters() {
        test(
                new String[] {"none,filters(package1,package2)"},
                0,
                Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }

    /**
     * Test {@code %throwable{short,filters(package1,package2)} }
     */
    @Test
    void testSingleOptionShortAndMultipleFilters() {
        test(
                new String[] {"short,filters(package1,package2)"},
                2,
                Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }

    /**
     * Test {@code %throwable{none,filters(package1,package2)} }
     */
    @Test
    void testSingleOptionDepthAndMultipleFilters() {
        test(
                new String[] {"10,filters(package1,package2)"},
                10,
                Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }
}
