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

import static org.apache.logging.log4j.core.pattern.AnsiEscape.BG_RED;
import static org.apache.logging.log4j.core.pattern.AnsiEscape.BOLD;
import static org.apache.logging.log4j.core.pattern.AnsiEscape.RED;
import static org.apache.logging.log4j.core.pattern.AnsiEscape.WHITE;
import static org.apache.logging.log4j.core.pattern.AnsiEscape.YELLOW;
import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Renders an input as ANSI escaped output.
 * <p>
 * Uses the
 * <a href="https://www.javadoc.io/doc/org.jline/jline/latest/org/jline/jansi/AnsiRenderer.html">JLine AnsiRenderer syntax</a>
 * to render a message into an ANSI escaped string.
 * </p>
 * <p>
 * The default syntax for embedded ANSI codes is:
 * </p>
 * <pre>
 *   &#64;|<em>code</em>(,<em>code</em>)* <em>text</em>|@
 * </pre>
 *
 * For example, to render the message {@code "Hello"} in green, use:
 *
 * <pre>
 *   &#64;|green Hello|@
 * </pre>
 *
 * To render the message {@code "Hello"} in bold and red, use:
 *
 * <pre>
 *   &#64;|bold,red Warning!|@
 * </pre>
 *
 * You can also define custom style names in the configuration with the syntax:
 *
 * <pre>
 * %message{ansi}{StyleName=value(,value)*( StyleName=value(,value)*)*}%n
 * </pre>
 *
 * For example:
 *
 * <pre>
 * %message{ansi}{WarningStyle=red,bold KeyStyle=white ValueStyle=blue}%n
 * </pre>
 *
 * The call site can look like this:
 *
 * <pre>
 * logger.info("@|KeyStyle {}|@ = @|ValueStyle {}|@", entry.getKey(), entry.getValue());
 * </pre>
 *
 * <p>
 *     <strong>Note:</strong> this class was originally copied and then heavily modified from
 *     <a href="https://www.javadoc.io/doc/org.jline/jline/latest/org/jline/jansi/AnsiRenderer.html">JAnsi/JLine AnsiRenderer</a>,
 *     licensed under an Apache Software License, version 2.0.
 * </p>
 */
public final class JAnsiTextRenderer implements TextRenderer {

    private static final Logger LOGGER = StatusLogger.getLogger();

    public static final Map<String, String> DefaultExceptionStyleMap;
    static final Map<String, String> DEFAULT_MESSAGE_STYLE_MAP;
    private static final Map<String, Map<String, String>> PREFEDINED_STYLE_MAPS;

    private static final String BEGIN_TOKEN = "@|";
    private static final String END_TOKEN = "|@";
    // The length of AnsiEscape.CSI
    private static final int CSI_LENGTH = 2;

    private static Map.Entry<String, String> entry(final String name, final AnsiEscape... codes) {
        final StringBuilder sb = new StringBuilder(AnsiEscape.CSI.getCode());
        for (final AnsiEscape code : codes) {
            sb.append(code.getCode());
        }
        return new AbstractMap.SimpleImmutableEntry<>(name, sb.toString());
    }

    @SafeVarargs
    private static <V> Map<String, V> ofEntries(final Map.Entry<String, V>... entries) {
        final Map<String, V> map = new HashMap<>(entries.length);
        for (final Map.Entry<String, V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(map);
    }

    static {
        // Default style: Spock
        final Map<String, String> spock = ofEntries(
                entry("Prefix", WHITE),
                entry("Name", BG_RED, WHITE),
                entry("NameMessageSeparator", BG_RED, WHITE),
                entry("Message", BG_RED, WHITE, BOLD),
                entry("At", WHITE),
                entry("CauseLabel", WHITE),
                entry("Text", WHITE),
                entry("More", WHITE),
                entry("Suppressed", WHITE),
                // StackTraceElement
                entry("StackTraceElement.ClassLoaderName", WHITE),
                entry("StackTraceElement.ClassLoaderSeparator", WHITE),
                entry("StackTraceElement.ModuleName", WHITE),
                entry("StackTraceElement.ModuleVersionSeparator", WHITE),
                entry("StackTraceElement.ModuleVersion", WHITE),
                entry("StackTraceElement.ModuleNameSeparator", WHITE),
                entry("StackTraceElement.ClassName", YELLOW),
                entry("StackTraceElement.ClassMethodSeparator", YELLOW),
                entry("StackTraceElement.MethodName", YELLOW),
                entry("StackTraceElement.NativeMethod", YELLOW),
                entry("StackTraceElement.FileName", RED),
                entry("StackTraceElement.LineNumber", RED),
                entry("StackTraceElement.Container", RED),
                entry("StackTraceElement.ContainerSeparator", WHITE),
                entry("StackTraceElement.UnknownSource", RED),
                // ExtraClassInfo
                entry("ExtraClassInfo.Inexact", YELLOW),
                entry("ExtraClassInfo.Container", YELLOW),
                entry("ExtraClassInfo.ContainerSeparator", YELLOW),
                entry("ExtraClassInfo.Location", YELLOW),
                entry("ExtraClassInfo.Version", YELLOW));

        // Style: Kirk
        final Map<String, String> kirk = ofEntries(
                entry("Prefix", WHITE),
                entry("Name", BG_RED, YELLOW, BOLD),
                entry("NameMessageSeparator", BG_RED, YELLOW),
                entry("Message", BG_RED, WHITE, BOLD),
                entry("At", WHITE),
                entry("CauseLabel", WHITE),
                entry("Text", WHITE),
                entry("More", WHITE),
                entry("Suppressed", WHITE),
                // StackTraceElement
                entry("StackTraceElement.ClassLoaderName", WHITE),
                entry("StackTraceElement.ClassLoaderSeparator", WHITE),
                entry("StackTraceElement.ModuleName", WHITE),
                entry("StackTraceElement.ModuleVersionSeparator", WHITE),
                entry("StackTraceElement.ModuleVersion", WHITE),
                entry("StackTraceElement.ModuleNameSeparator", WHITE),
                entry("StackTraceElement.ClassName", BG_RED, WHITE),
                entry("StackTraceElement.ClassMethodSeparator", BG_RED, YELLOW),
                entry("StackTraceElement.MethodName", BG_RED, YELLOW),
                entry("StackTraceElement.NativeMethod", BG_RED, YELLOW),
                entry("StackTraceElement.FileName", RED),
                entry("StackTraceElement.LineNumber", RED),
                entry("StackTraceElement.Container", RED),
                entry("StackTraceElement.ContainerSeparator", WHITE),
                entry("StackTraceElement.UnknownSource", RED),
                // ExtraClassInfo
                entry("ExtraClassInfo.Inexact", YELLOW),
                entry("ExtraClassInfo.Container", WHITE),
                entry("ExtraClassInfo.ContainerSeparator", WHITE),
                entry("ExtraClassInfo.Location", YELLOW),
                entry("ExtraClassInfo.Version", YELLOW));

        // Save
        DefaultExceptionStyleMap = spock;
        DEFAULT_MESSAGE_STYLE_MAP = Collections.emptyMap();
        Map<String, Map<String, String>> predefinedStyleMaps = new HashMap<>();
        predefinedStyleMaps.put("Spock", spock);
        predefinedStyleMaps.put("Kirk", kirk);
        PREFEDINED_STYLE_MAPS = Collections.unmodifiableMap(predefinedStyleMaps);
    }

    private final String beginToken;
    private final int beginTokenLen;
    private final String endToken;
    private final int endTokenLen;
    private final Map<String, String> styleMap;

    public JAnsiTextRenderer(final String[] formats, final Map<String, String> defaultStyleMap) {
        // The format string is a list of whitespace-separated expressions:
        // Key=AnsiEscape(,AnsiEscape)*
        if (formats.length > 1) {
            final String stylesStr = formats[1];
            final Map<String, String> map = AnsiEscape.createMap(
                    stylesStr.split("\\s", -1), new String[] {"BeginToken", "EndToken", "Style"}, ",");

            // Handle the special tokens
            beginToken = Objects.toString(map.remove("BeginToken"), BEGIN_TOKEN);
            endToken = Objects.toString(map.remove("EndToken"), END_TOKEN);
            final String predefinedStyle = map.remove("Style");

            // Create style map
            final Map<String, String> styleMap = new HashMap<>(map.size() + defaultStyleMap.size());
            defaultStyleMap.forEach((k, v) -> styleMap.put(toRootUpperCase(k), v));
            if (predefinedStyle != null) {
                final Map<String, String> predefinedMap = PREFEDINED_STYLE_MAPS.get(predefinedStyle);
                if (predefinedMap != null) {
                    map.putAll(predefinedMap);
                } else {
                    LOGGER.warn(
                            "Unknown predefined map name {}, pick one of {}",
                            predefinedStyle,
                            PREFEDINED_STYLE_MAPS.keySet());
                }
            }
            styleMap.putAll(map);
            this.styleMap = Collections.unmodifiableMap(styleMap);
        } else {
            beginToken = BEGIN_TOKEN;
            endToken = END_TOKEN;
            this.styleMap = Collections.unmodifiableMap(defaultStyleMap);
        }
        beginTokenLen = beginToken.length();
        endTokenLen = endToken.length();
    }

    /**
     * Renders the given input with the given names which can be ANSI code names or Log4j style names.
     *
     * @param input
     *            The input to render
     * @param styleNames
     *            ANSI code names or Log4j style names.
     */
    private void render(final String input, final StringBuilder output, final String... styleNames) {
        boolean first = true;
        for (final String styleName : styleNames) {
            final String escape = styleMap.get(toRootUpperCase(styleName));
            if (escape != null) {
                merge(escape, output, first);
            } else {
                merge(AnsiEscape.createSequence(styleName), output, first);
            }
            first = false;
        }
        output.append(input).append(AnsiEscape.getDefaultStyle());
    }

    private static void merge(final String escapeSequence, final StringBuilder output, final boolean first) {
        if (first) {
            output.append(escapeSequence);
        } else {
            // Delete the trailing AnsiEscape.SUFFIX
            output.setLength(output.length() - 1);
            output.append(AnsiEscape.SEPARATOR.getCode());
            output.append(escapeSequence.substring(CSI_LENGTH));
        }
    }

    // EXACT COPY OF StringBuilder version of the method but typed as String for input
    @Override
    public void render(final String input, final StringBuilder output, final String styleName)
            throws IllegalArgumentException {
        render(input, output, styleName.split(",", -1));
    }

    @Override
    public void render(final StringBuilder input, final StringBuilder output) throws IllegalArgumentException {
        int pos = 0;
        int beginTokenPos, endTokenPos;

        while (true) {
            beginTokenPos = input.indexOf(beginToken, pos);
            if (beginTokenPos == -1) {
                output.append(pos == 0 ? input : input.substring(pos, input.length()));
                return;
            }
            output.append(input.substring(pos, beginTokenPos));
            endTokenPos = input.indexOf(endToken, beginTokenPos);

            if (endTokenPos == -1) {
                LOGGER.warn(
                        "Missing matching end token {} for token at position {}: '{}'", endToken, beginTokenPos, input);
                output.append(beginTokenPos == 0 ? input : input.substring(beginTokenPos, input.length()));
                return;
            }
            beginTokenPos += beginTokenLen;
            final String spec = input.substring(beginTokenPos, endTokenPos);

            final String[] items = spec.split("\\s", 2);
            if (items.length == 1) {
                LOGGER.warn("Missing argument in ANSI escape specification '{}'", spec);
                output.append(beginToken).append(spec).append(endToken);
            } else {
                render(items[1], output, items[0].split(",", -1));
            }
            pos = endTokenPos + endTokenLen;
        }
    }

    public Map<String, String> getStyleMap() {
        return styleMap;
    }

    @Override
    public String toString() {
        return "AnsiMessageRenderer [beginToken=" + beginToken + ", beginTokenLen=" + beginTokenLen + ", endToken="
                + endToken + ", endTokenLen=" + endTokenLen + ", styleMap=" + styleMap + "]";
    }
}
