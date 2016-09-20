/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.pattern.JAnsiTextRenderer;
import org.apache.logging.log4j.core.pattern.TextRenderer;
import org.apache.logging.log4j.util.Strings;
import org.fusesource.jansi.AnsiRenderer.Code;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@code ThrowableFormatOptions}.
 */
public final class ThrowableFormatOptionsTest {

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
    private static ThrowableFormatOptions test(final String[] options, final int expectedLines,
            final String expectedSeparator, final List<String> expectedPackages) {
        final ThrowableFormatOptions tfo = ThrowableFormatOptions.newInstance(options);
        assertEquals("getLines", expectedLines, tfo.getLines());
        assertEquals("getSeparator", expectedSeparator, tfo.getSeparator());
        assertEquals("getPackages", expectedPackages, tfo.getIgnorePackages());
        assertEquals("allLines", expectedLines == Integer.MAX_VALUE, tfo.allLines());
        assertEquals("anyLines", expectedLines != 0, tfo.anyLines());
        assertEquals("minLines", 0, tfo.minLines(0));
        assertEquals("minLines", expectedLines, tfo.minLines(Integer.MAX_VALUE));
        assertEquals("hasPackages", expectedPackages != null && !expectedPackages.isEmpty(), tfo.hasPackages());
        assertNotNull("toString", tfo.toString());
        return tfo;
    }

    /**
     * Test {@code %throwable } with null options.
     */
    @Test
    public void testNull() {
        test(null, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable }
     */
    @Test
    public void testEmpty() {
        test(new String[] {}, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{} } with null option value.
     */
    @Test
    public void testOneNullElement() {
        test(new String[] { null }, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{} }
     */
    @Test
    public void testOneEmptyElement() {
        test(new String[] { "" }, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{full} }
     */
    @Test
    public void testFull() {
        test(new String[] { "full" }, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{full}{ansi} }
     */
    @Test
    public void testFullAnsi() {
        final ThrowableFormatOptions tfo = test(new String[] { "full", "ansi" },
                Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
        testFullAnsiEmptyConfig(tfo);
    }

    /**
     * Test {@code %throwable{full}{ansi} }
     */
    @Test
    public void testFullAnsiEmptyConfig() {
        final ThrowableFormatOptions tfo = test(new String[] { "full", "ansi()" },
                Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
        testFullAnsiEmptyConfig(tfo);
    }

    private void testFullAnsiEmptyConfig(final ThrowableFormatOptions tfo) {
        final TextRenderer textRenderer = tfo.getTextRenderer();
        Assert.assertNotNull(textRenderer);
        Assert.assertTrue(textRenderer instanceof JAnsiTextRenderer);
        final JAnsiTextRenderer jansiRenderer = (JAnsiTextRenderer) textRenderer;
        final Map<String, Code[]> styleMap = jansiRenderer.getStyleMap();
        // We have defaults
        Assert.assertFalse(styleMap.isEmpty());
        Assert.assertNotNull(styleMap.get("Name"));
    }

    /**
     * Test {@code %throwable{full}{ansi(Warning=red))} }
     */
    @Test
    public void testFullAnsiWithCustomStyle() {
        final ThrowableFormatOptions tfo = test(new String[] { "full", "ansi(Warning=red)" },
                Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
        final TextRenderer textRenderer = tfo.getTextRenderer();
        Assert.assertNotNull(textRenderer);
        Assert.assertTrue(textRenderer instanceof JAnsiTextRenderer);
        final JAnsiTextRenderer jansiRenderer = (JAnsiTextRenderer) textRenderer;
        final Map<String, Code[]> styleMap = jansiRenderer.getStyleMap();
        Assert.assertArrayEquals(new Code[] { Code.RED }, styleMap.get("Warning"));
    }

    /**
     * Test {@code %throwable{full}{ansi(Warning=red Key=blue Value=cyan))} }
     */
    @Test
    public void testFullAnsiWithCustomStyles() {
        final ThrowableFormatOptions tfo = test(new String[] { "full", "ansi(Warning=red Key=blue Value=cyan)" },
                Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
        final TextRenderer textRenderer = tfo.getTextRenderer();
        Assert.assertNotNull(textRenderer);
        Assert.assertTrue(textRenderer instanceof JAnsiTextRenderer);
        final JAnsiTextRenderer jansiRenderer = (JAnsiTextRenderer) textRenderer;
        final Map<String, Code[]> styleMap = jansiRenderer.getStyleMap();
        Assert.assertArrayEquals(new Code[] { Code.RED }, styleMap.get("Warning"));
        Assert.assertArrayEquals(new Code[] { Code.BLUE }, styleMap.get("Key"));
        Assert.assertArrayEquals(new Code[] { Code.CYAN }, styleMap.get("Value"));
    }

    /**
     * Test {@code %throwable{full}{ansi(Warning=red Key=blue,bg_red Value=cyan,bg_black,underline)} }
     */
    @Test
    public void testFullAnsiWithCustomComplexStyles() {
        final ThrowableFormatOptions tfo = test(
                new String[] { "full", "ansi(Warning=red Key=blue,bg_red Value=cyan,bg_black,underline)" }, Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR, null);
        final TextRenderer textRenderer = tfo.getTextRenderer();
        Assert.assertNotNull(textRenderer);
        Assert.assertTrue(textRenderer instanceof JAnsiTextRenderer);
        final JAnsiTextRenderer jansiRenderer = (JAnsiTextRenderer) textRenderer;
        final Map<String, Code[]> styleMap = jansiRenderer.getStyleMap();
        Assert.assertArrayEquals(new Code[] { Code.RED }, styleMap.get("Warning"));
        Assert.assertArrayEquals(new Code[] { Code.BLUE, Code.BG_RED }, styleMap.get("Key"));
        Assert.assertArrayEquals(new Code[] { Code.CYAN, Code.BG_BLACK, Code.UNDERLINE }, styleMap.get("Value"));
    }

    /**
     * Test {@code %throwable{none} }
     */
    @Test
    public void testNone() {
        test(new String[] { "none" }, 0, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{short} }
     */
    @Test
    public void testShort() {
        test(new String[] { "short" }, 2, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{10} }
     */
    @Test
    public void testDepth() {
        test(new String[] { "10" }, 10, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{separator(|)} }
     */
    @Test
    public void testSeparator() {
        test(new String[] { "separator(|)" }, Integer.MAX_VALUE, "|", null);
    }

    /**
     * Test {@code %throwable{separator()} }
     */
    @Test
    public void testSeparatorAsEmpty() {
        test(new String[] { "separator()" }, Integer.MAX_VALUE, Strings.EMPTY, null);
    }

    /**
     * Test {@code %throwable{separator(\n)} }
     */
    @Test
    public void testSeparatorAsDefaultLineSeparator() {
        test(new String[] { "separator(" + Strings.LINE_SEPARATOR + ')' }, Integer.MAX_VALUE,
                Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{separator(|)} }
     */
    @Test
    public void testSeparatorAsMultipleCharacters() {
        test(new String[] { "separator( | )" }, Integer.MAX_VALUE, " | ", null);
    }

    /**
     * Test {@code %throwable{full}{separator(|)} }
     */
    @Test
    public void testFullAndSeparator() {
        test(new String[] { "full", "separator(|)" }, Integer.MAX_VALUE, "|", null);
    }

    /**
     * Test {@code %throwable{none}{separator(|)} }
     */
    @Test
    public void testNoneAndSeparator() {
        test(new String[] { "none", "separator(|)" }, 0, "|", null);
    }

    /**
     * Test {@code %throwable{short}{separator(|)} }
     */
    @Test
    public void testShortAndSeparator() {
        test(new String[] { "short", "separator(|)" }, 2, "|", null);
    }

    /**
     * Test {@code %throwable{10}{separator(|)} }
     */
    @Test
    public void testDepthAndSeparator() {
        test(new String[] { "10", "separator(|)" }, 10, "|", null);
    }

    /**
     * Test {@code %throwable{filters(packages)} }
     */
    @Test
    public void testFilters() {
        test(new String[] { "filters(packages)" }, Integer.MAX_VALUE, Strings.LINE_SEPARATOR,
                Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{filters()} }
     */
    @Test
    public void testFiltersAsEmpty() {
        test(new String[] { "filters()" }, Integer.MAX_VALUE, Strings.LINE_SEPARATOR, null);
    }

    /**
     * Test {@code %throwable{filters(package1,package2)} }
     */
    @Test
    public void testFiltersAsMultiplePackages() {
        test(new String[] { "filters(package1,package2)" }, Integer.MAX_VALUE, Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }

    /**
     * Test {@code %throwable{full}{filters(packages)} }
     */
    @Test
    public void testFullAndFilters() {
        test(new String[] { "full", "filters(packages)" }, Integer.MAX_VALUE, Strings.LINE_SEPARATOR,
                Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{none}{filters(packages)} }
     */
    @Test
    public void testNoneAndFilters() {
        test(new String[] { "none", "filters(packages)" }, 0, Strings.LINE_SEPARATOR, Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{short}{filters(packages)} }
     */
    @Test
    public void testShortAndFilters() {
        test(new String[] { "short", "filters(packages)" }, 2, Strings.LINE_SEPARATOR, Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{10}{filters(packages)} }
     */
    @Test
    public void testDepthAndFilters() {
        test(new String[] { "10", "filters(packages)" }, 10, Strings.LINE_SEPARATOR, Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{full}{separator(|)}{filters(packages)} }
     */
    @Test
    public void testFullAndSeparatorAndFilters() {
        test(new String[] { "full", "separator(|)", "filters(packages)" }, Integer.MAX_VALUE, "|",
                Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{none}{separator(|)}{filters(packages)} }
     */
    @Test
    public void testNoneAndSeparatorAndFilters() {
        test(new String[] { "none", "separator(|)", "filters(packages)" }, 0, "|", Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{short}{separator(|)}{filters(packages)} }
     */
    @Test
    public void testShortAndSeparatorAndFilters() {
        test(new String[] { "short", "separator(|)", "filters(packages)" }, 2, "|", Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{10}{separator(|)}{filters(packages)} }
     */
    @Test
    public void testDepthAndSeparatorAndFilters() {
        test(new String[] { "10", "separator(|)", "filters(packages)" }, 10, "|", Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{full,filters(packages)} }
     */
    @Test
    public void testSingleOptionFullAndFilters() {
        test(new String[] { "full,filters(packages)" }, Integer.MAX_VALUE, Strings.LINE_SEPARATOR,
                Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{none,filters(packages)} }
     */
    @Test
    public void testSingleOptionNoneAndFilters() {
        test(new String[] { "none,filters(packages)" }, 0, Strings.LINE_SEPARATOR, Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{short,filters(packages)} }
     */
    @Test
    public void testSingleOptionShortAndFilters() {
        test(new String[] { "short,filters(packages)" }, 2, Strings.LINE_SEPARATOR, Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{none,filters(packages)} }
     */
    @Test
    public void testSingleOptionDepthAndFilters() {
        test(new String[] { "10,filters(packages)" }, 10, Strings.LINE_SEPARATOR, Arrays.asList("packages"));
    }

    /**
     * Test {@code %throwable{full,filters(package1,package2)} }
     */
    @Test
    public void testSingleOptionFullAndMultipleFilters() {
        test(new String[] { "full,filters(package1,package2)" }, Integer.MAX_VALUE, Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }

    /**
     * Test {@code %throwable{none,filters(package1,package2)} }
     */
    @Test
    public void testSingleOptionNoneAndMultipleFilters() {
        test(new String[] { "none,filters(package1,package2)" }, 0, Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }

    /**
     * Test {@code %throwable{short,filters(package1,package2)} }
     */
    @Test
    public void testSingleOptionShortAndMultipleFilters() {
        test(new String[] { "short,filters(package1,package2)" }, 2, Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }

    /**
     * Test {@code %throwable{none,filters(package1,package2)} }
     */
    @Test
    public void testSingleOptionDepthAndMultipleFilters() {
        test(new String[] { "10,filters(package1,package2)" }, 10, Strings.LINE_SEPARATOR,
                Arrays.asList("package1", "package2"));
    }
}
