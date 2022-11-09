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
package org.apache.logging.log4j.core.pattern;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 * Converts text into ANSI escape sequences.
 * <p>
 * The names for colors and attributes are standard, but the exact shade/hue/value of colors are not, and depend on the
 * device used to display them.
 * </p>
 */
public enum AnsiEscape {

    /**
     * The Control Sequence Introducer (or Control Sequence Initiator).
     * <p>
     * Most sequences are more than two characters and start with the characters ESC and [ (the left bracket).
     * </p>
     */
    CSI("\u001b["),

    /**
     * Escape suffix.
     */
    SUFFIX("m"),

    /**
     * Escape separator.
     */
    SEPARATOR(";"),

    /**
     * Normal general attribute.
     */
    NORMAL("0"),

    /**
     * Bright general attribute.
     */
    BRIGHT("1"),

    /**
     * Dim general attribute.
     */
    DIM("2"),

    /**
     * Underline general attribute.
     */
    UNDERLINE("3"),

    /**
     * Blink general attribute.
     */
    BLINK("5"),

    /**
     * Reverse general attribute.
     */
    REVERSE("7"),

    /**
     * Normal general attribute.
     */
    HIDDEN("8"),

    /**
     * Black foreground color.
     */
    BLACK("30"),

    /**
     * Black foreground color.
     */
    FG_BLACK("30"),

    /**
     * Red foreground color.
     */
    RED("31"),

    /**
     * Red foreground color.
     */
    FG_RED("31"),

    /**
     * Green foreground color.
     */
    GREEN("32"),

    /**
     * Green foreground color.
     */
    FG_GREEN("32"),

    /**
     * Yellow foreground color.
     */
    YELLOW("33"),

    /**
     * Yellow foreground color.
     */
    FG_YELLOW("33"),

    /**
     * Blue foreground color.
     */
    BLUE("34"),

    /**
     * Blue foreground color.
     */
    FG_BLUE("34"),

    /**
     * Magenta foreground color.
     */
    MAGENTA("35"),

    /**
     * Magenta foreground color.
     */
    FG_MAGENTA("35"),

    /**
     * Cyan foreground color.
     */
    CYAN("36"),

    /**
     * Cyan foreground color.
     */
    FG_CYAN("36"),

    /**
     * White foreground color.
     */
    WHITE("37"),

    /**
     * White foreground color.
     */
    FG_WHITE("37"),

    /**
     * Default foreground color.
     */
    DEFAULT("39"),

    /**
     * Default foreground color.
     */
    FG_DEFAULT("39"),

    /**
     * Black background color.
     */
    BG_BLACK("40"),

    /**
     * Red background color.
     */
    BG_RED("41"),

    /**
     * Green background color.
     */
    BG_GREEN("42"),

    /**
     * Yellow background color.
     */
    BG_YELLOW("43"),

    /**
     * Blue background color.
     */
    BG_BLUE("44"),

    /**
     * Magenta background color.
     */
    BG_MAGENTA("45"),

    /**
     * Cyan background color.
     */
    BG_CYAN("46"),

    /**
     * White background color.
     */
    BG_WHITE("47");

    private static final String DEFAULT_STYLE = CSI.getCode() + SUFFIX.getCode();

    private final String code;

    AnsiEscape(final String code) {
        this.code = code;
    }

    /**
     * Gets the default style.
     *
     * @return the default style
     */
    public static String getDefaultStyle() {
        return DEFAULT_STYLE;
    }

    /**
     * Gets the escape code.
     *
     * @return the escape code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Creates a Map from a source array where values are ANSI escape sequences. The format is:
     *
     * <pre>
     * Key1=Value, Key2=Value, ...
     * </pre>
     *
     * For example:
     *
     * <pre>
     * ERROR=red bold, WARN=yellow bold, INFO=green, ...
     * </pre>
     *
     * You can use whitespace around the comma and equal sign. The names in values MUST come from the
     * {@linkplain AnsiEscape} enum, case is normalized to upper-case internally.
     *
     * @param values the source string to parse.
     * @param dontEscapeKeys do not escape these keys, leave the values as is in the map
     * @return a new map
     */
    public static Map<String, String> createMap(final String values, final String[] dontEscapeKeys) {
        return createMap(values.split(Patterns.COMMA_SEPARATOR), dontEscapeKeys);
    }

    /**
     * Creates a Map from a source array where values are ANSI escape sequences. Each array entry must be in the format:
     *
     * <pre>
     * Key1 = Value
     * </pre>
     *
     * For example:
     *
     * <pre>
     * ERROR=red bold
     * </pre>
     *
     * You can use whitespace around the equal sign and between the value elements. The names in values MUST come from
     * the {@linkplain AnsiEscape} enum, case is normalized to upper-case internally.
     *
     * @param values
     *            the source array to parse.
     * @param dontEscapeKeys
     *            do not escape these keys, leave the values as is in the map
     * @return a new map
     */
    public static Map<String, String> createMap(final String[] values, final String[] dontEscapeKeys) {
        final String[] sortedIgnoreKeys = dontEscapeKeys != null ? dontEscapeKeys.clone() : new String[0];
        Arrays.sort(sortedIgnoreKeys);
        final Map<String, String> map = new HashMap<>();
        for (final String string : values) {
            final String[] keyValue = string.split(Patterns.toWhitespaceSeparator("="));
            if (keyValue.length > 1) {
                final String key = keyValue[0].toUpperCase(Locale.ENGLISH);
                final String value = keyValue[1];
                final boolean escape = Arrays.binarySearch(sortedIgnoreKeys, key) < 0;
                map.put(key, escape ? createSequence(value.split("\\s")) : value);
            }
        }
        return map;
    }

    /**
     * Creates an ANSI escape sequence from the given {@linkplain AnsiEscape} names.
     *
     * @param names
     *            {@linkplain AnsiEscape} names.
     * @return An ANSI escape sequence.
     */
    public static String createSequence(final String... names) {
        if (names == null) {
            return getDefaultStyle();
        }
        final StringBuilder sb = new StringBuilder(AnsiEscape.CSI.getCode());
        boolean first = true;
        for (final String name : names) {
            try {
                final AnsiEscape escape = EnglishEnums.valueOf(AnsiEscape.class, name.trim());
                if (!first) {
                    sb.append(AnsiEscape.SEPARATOR.getCode());
                }
                first = false;
                sb.append(escape.getCode());
            } catch (final Exception ex) {
                // Ignore the error.
            }
        }
        sb.append(AnsiEscape.SUFFIX.getCode());
        return sb.toString();
    }

}
