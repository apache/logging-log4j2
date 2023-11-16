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
package org.apache.logging.log4j.core.pattern;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.EnglishEnums;
import org.apache.logging.log4j.util.Strings;

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
     *
     * @deprecated This attribute sets font-weight as "bold" and doesn't set color brightness. Use BOLD if you
     * need to change font-weight and BRIGHT_* to use a bright color.
     *
     */
    BRIGHT("1"),

    /**
     * Bold general attribute.
     */
    BOLD("1"),

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
    BG_WHITE("47"),

    /**
     * Bright black foreground color.
     */
    BRIGHT_BLACK("90"),

    /**
     * Bright black foreground color.
     */
    FG_BRIGHT_BLACK("90"),

    /**
     * Bright red foreground color.
     */
    BRIGHT_RED("91"),

    /**
     * Bright red foreground color.
     */
    FG_BRIGHT_RED("91"),

    /**
     * Bright green foreground color.
     */
    BRIGHT_GREEN("92"),

    /**
     * Bright green foreground color.
     */
    FG_BRIGHT_GREEN("92"),

    /**
     * Bright yellow foreground color.
     */
    BRIGHT_YELLOW("93"),

    /**
     * Bright yellow foreground color.
     */
    FG_BRIGHT_YELLOW("93"),

    /**
     * Bright blue foreground color.
     */
    BRIGHT_BLUE("94"),

    /**
     * Bright blue foreground color.
     */
    FG_BRIGHT_BLUE("94"),

    /**
     * Bright magenta foreground color.
     */
    BRIGHT_MAGENTA("95"),

    /**
     * Bright magenta foreground color.
     */
    FG_BRIGHT_MAGENTA("95"),

    /**
     * Bright cyan foreground color.
     */
    BRIGHT_CYAN("96"),

    /**
     * Bright cyan foreground color.
     */
    FG_BRIGHT_CYAN("96"),

    /**
     * Bright white foreground color.
     */
    BRIGHT_WHITE("97"),

    /**
     * Bright white foreground color.
     */
    FG_BRIGHT_WHITE("97"),

    /**
     * Bright black background color.
     */
    BG_BRIGHT_BLACK("100"),

    /**
     * Bright red background color.
     */
    BG_BRIGHT_RED("101"),

    /**
     * Bright green background color.
     */
    BG_BRIGHT_GREEN("102"),

    /**
     * Bright yellow background color.
     */
    BG_BRIGHT_YELLOW("103"),

    /**
     * Bright blue background color.
     */
    BG_BRIGHT_BLUE("104"),

    /**
     * Bright magenta background color.
     */
    BG_BRIGHT_MAGENTA("105"),

    /**
     * Bright cyan background color.
     */
    BG_BRIGHT_CYAN("106"),

    /**
     * Bright white background color.
     */
    BG_BRIGHT_WHITE("107");
    private static final StatusLogger LOGGER = StatusLogger.getLogger();

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
        final String[] sortedIgnoreKeys = dontEscapeKeys != null ? dontEscapeKeys.clone() : Strings.EMPTY_ARRAY;
        Arrays.sort(sortedIgnoreKeys);
        final Map<String, String> map = new HashMap<>();
        for (final String string : values) {
            final String[] keyValue = string.split(Patterns.toWhitespaceSeparator("="));
            if (keyValue.length > 1) {
                final String key = toRootUpperCase(keyValue[0]);
                final String value = keyValue[1];
                final boolean escape = Arrays.binarySearch(sortedIgnoreKeys, key) < 0;
                map.put(key, escape ? createSequence(value.split("\\s")) : value);
            } else {
                LOGGER.warn("Syntax error, missing '=': Expected \"{KEY1=VALUE, KEY2=VALUE, ...}");
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
                // GitHub Issue #1202
                if (name.startsWith(PatternParser.DISABLE_ANSI) || name.startsWith(PatternParser.NO_CONSOLE_NO_ANSI)) {
                    continue;
                }
                if (!first) {
                    sb.append(AnsiEscape.SEPARATOR.getCode());
                }
                first = false;
                String hexColor = null;
                final String trimmedName = toRootUpperCase(name.trim());
                if (trimmedName.startsWith("#")) {
                    sb.append("38");
                    sb.append(SEPARATOR.getCode());
                    sb.append("2");
                    sb.append(SEPARATOR.getCode());
                    hexColor = trimmedName;
                } else if (trimmedName.startsWith("FG_#")) {
                    sb.append("38");
                    sb.append(SEPARATOR.getCode());
                    sb.append("2");
                    sb.append(SEPARATOR.getCode());
                    hexColor = trimmedName.substring(3);
                } else if (trimmedName.startsWith("BG_#")) {
                    sb.append("48");
                    sb.append(SEPARATOR.getCode());
                    sb.append("2");
                    sb.append(SEPARATOR.getCode());
                    hexColor = trimmedName.substring(3);
                }
                if (hexColor != null) {
                    sb.append(Integer.valueOf(hexColor.substring(1, 3), 16)); // r
                    sb.append(SEPARATOR.getCode());
                    sb.append(Integer.valueOf(hexColor.substring(3, 5), 16)); // g
                    sb.append(SEPARATOR.getCode());
                    sb.append(Integer.valueOf(hexColor.substring(5, 7), 16)); // b
                    // no separator at the end
                } else {
                    final AnsiEscape escape = EnglishEnums.valueOf(AnsiEscape.class, trimmedName);
                    sb.append(escape.getCode());
                }
            } catch (final Exception ex) {
                StatusLogger.getLogger().warn("The style attribute {} is incorrect.", name, ex);
            }
        }
        sb.append(AnsiEscape.SUFFIX.getCode());
        return sb.toString();
    }
}
