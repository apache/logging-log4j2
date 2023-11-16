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
import static org.fusesource.jansi.AnsiRenderer.Code.BG_RED;
import static org.fusesource.jansi.AnsiRenderer.Code.BOLD;
import static org.fusesource.jansi.AnsiRenderer.Code.RED;
import static org.fusesource.jansi.AnsiRenderer.Code.WHITE;
import static org.fusesource.jansi.AnsiRenderer.Code.YELLOW;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.status.StatusLogger;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiRenderer;
import org.fusesource.jansi.AnsiRenderer.Code;

/**
 * Renders an input as ANSI escaped output.
 *
 * Uses the JAnsi rendering syntax as the default to render a message into an ANSI escaped string.
 *
 * The default syntax for embedded ANSI codes is:
 *
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
 * Note: This class originally copied and then heavily modified code from JAnsi's AnsiRenderer (which is licensed as
 * Apache 2.0.)
 *
 * @see AnsiRenderer
 */
public final class JAnsiTextRenderer implements TextRenderer {

    public static final Map<String, Code[]> DefaultExceptionStyleMap;
    static final Map<String, Code[]> DefaultMessageStyleMap;
    private static final Map<String, Map<String, Code[]>> PrefedinedStyleMaps;

    private static void put(final Map<String, Code[]> map, final String name, final Code... codes) {
        map.put(name, codes);
    }

    static {
        final Map<String, Map<String, Code[]>> tempPreDefs = new HashMap<>();
        // Default style: Spock
        {
            // TODO Should the keys be in an enum?
            final Map<String, Code[]> map = new HashMap<>();
            put(map, "Prefix", WHITE);
            put(map, "Name", BG_RED, WHITE);
            put(map, "NameMessageSeparator", BG_RED, WHITE);
            put(map, "Message", BG_RED, WHITE, BOLD);
            put(map, "At", WHITE);
            put(map, "CauseLabel", WHITE);
            put(map, "Text", WHITE);
            put(map, "More", WHITE);
            put(map, "Suppressed", WHITE);
            // StackTraceElement
            put(map, "StackTraceElement.ClassLoaderName", WHITE);
            put(map, "StackTraceElement.ClassLoaderSeparator", WHITE);
            put(map, "StackTraceElement.ModuleName", WHITE);
            put(map, "StackTraceElement.ModuleVersionSeparator", WHITE);
            put(map, "StackTraceElement.ModuleVersion", WHITE);
            put(map, "StackTraceElement.ModuleNameSeparator", WHITE);
            put(map, "StackTraceElement.ClassName", YELLOW);
            put(map, "StackTraceElement.ClassMethodSeparator", YELLOW);
            put(map, "StackTraceElement.MethodName", YELLOW);
            put(map, "StackTraceElement.NativeMethod", YELLOW);
            put(map, "StackTraceElement.FileName", RED);
            put(map, "StackTraceElement.LineNumber", RED);
            put(map, "StackTraceElement.Container", RED);
            put(map, "StackTraceElement.ContainerSeparator", WHITE);
            put(map, "StackTraceElement.UnknownSource", RED);
            // ExtraClassInfo
            put(map, "ExtraClassInfo.Inexact", YELLOW);
            put(map, "ExtraClassInfo.Container", YELLOW);
            put(map, "ExtraClassInfo.ContainerSeparator", YELLOW);
            put(map, "ExtraClassInfo.Location", YELLOW);
            put(map, "ExtraClassInfo.Version", YELLOW);
            // Save
            DefaultExceptionStyleMap = Collections.unmodifiableMap(map);
            tempPreDefs.put("Spock", DefaultExceptionStyleMap);
        }
        // Style: Kirk
        {
            // TODO Should the keys be in an enum?
            final Map<String, Code[]> map = new HashMap<>();
            put(map, "Prefix", WHITE);
            put(map, "Name", BG_RED, YELLOW, BOLD);
            put(map, "NameMessageSeparator", BG_RED, YELLOW);
            put(map, "Message", BG_RED, WHITE, BOLD);
            put(map, "At", WHITE);
            put(map, "CauseLabel", WHITE);
            put(map, "Text", WHITE);
            put(map, "More", WHITE);
            put(map, "Suppressed", WHITE);
            // StackTraceElement
            put(map, "StackTraceElement.ClassLoaderName", WHITE);
            put(map, "StackTraceElement.ClassLoaderSeparator", WHITE);
            put(map, "StackTraceElement.ModuleName", WHITE);
            put(map, "StackTraceElement.ModuleVersionSeparator", WHITE);
            put(map, "StackTraceElement.ModuleVersion", WHITE);
            put(map, "StackTraceElement.ModuleNameSeparator", WHITE);
            put(map, "StackTraceElement.ClassName", BG_RED, WHITE);
            put(map, "StackTraceElement.ClassMethodSeparator", BG_RED, YELLOW);
            put(map, "StackTraceElement.MethodName", BG_RED, YELLOW);
            put(map, "StackTraceElement.NativeMethod", BG_RED, YELLOW);
            put(map, "StackTraceElement.FileName", RED);
            put(map, "StackTraceElement.LineNumber", RED);
            put(map, "StackTraceElement.Container", RED);
            put(map, "StackTraceElement.ContainerSeparator", WHITE);
            put(map, "StackTraceElement.UnknownSource", RED);
            // ExtraClassInfo
            put(map, "ExtraClassInfo.Inexact", YELLOW);
            put(map, "ExtraClassInfo.Container", WHITE);
            put(map, "ExtraClassInfo.ContainerSeparator", WHITE);
            put(map, "ExtraClassInfo.Location", YELLOW);
            put(map, "ExtraClassInfo.Version", YELLOW);
            // Save
            tempPreDefs.put("Kirk", Collections.unmodifiableMap(map));
        }
        {
            final Map<String, Code[]> temp = new HashMap<>();
            // TODO
            DefaultMessageStyleMap = Collections.unmodifiableMap(temp);
        }
        PrefedinedStyleMaps = Collections.unmodifiableMap(tempPreDefs);
    }

    private final String beginToken;
    private final int beginTokenLen;
    private final String endToken;
    private final int endTokenLen;
    private final Map<String, Code[]> styleMap;

    public JAnsiTextRenderer(final String[] formats, final Map<String, Code[]> defaultStyleMap) {
        String tempBeginToken = AnsiRenderer.BEGIN_TOKEN;
        String tempEndToken = AnsiRenderer.END_TOKEN;
        final Map<String, Code[]> map;
        if (formats.length > 1) {
            final String allStylesStr = formats[1];
            // Style def split
            final String[] allStyleAssignmentsArr = allStylesStr.split(" ");
            map = new HashMap<>(allStyleAssignmentsArr.length + defaultStyleMap.size());
            map.putAll(defaultStyleMap);
            for (final String styleAssignmentStr : allStyleAssignmentsArr) {
                final String[] styleAssignmentArr = styleAssignmentStr.split("=");
                if (styleAssignmentArr.length != 2) {
                    StatusLogger.getLogger()
                            .warn(
                                    "{} parsing style \"{}\", expected format: StyleName=Code(,Code)*",
                                    getClass().getSimpleName(),
                                    styleAssignmentStr);
                } else {
                    final String styleName = styleAssignmentArr[0];
                    final String codeListStr = styleAssignmentArr[1];
                    final String[] codeNames = codeListStr.split(",");
                    if (codeNames.length == 0) {
                        StatusLogger.getLogger()
                                .warn(
                                        "{} parsing style \"{}\", expected format: StyleName=Code(,Code)*",
                                        getClass().getSimpleName(),
                                        styleAssignmentStr);
                    } else {
                        switch (styleName) {
                            case "BeginToken":
                                tempBeginToken = codeNames[0];
                                break;
                            case "EndToken":
                                tempEndToken = codeNames[0];
                                break;
                            case "StyleMapName":
                                final String predefinedMapName = codeNames[0];
                                final Map<String, Code[]> predefinedMap = PrefedinedStyleMaps.get(predefinedMapName);
                                if (predefinedMap != null) {
                                    map.putAll(predefinedMap);
                                } else {
                                    StatusLogger.getLogger()
                                            .warn(
                                                    "Unknown predefined map name {}, pick one of {}",
                                                    predefinedMapName,
                                                    null);
                                }
                                break;
                            default:
                                final Code[] codes = new Code[codeNames.length];
                                for (int i = 0; i < codes.length; i++) {
                                    codes[i] = toCode(codeNames[i]);
                                }
                                map.put(styleName, codes);
                        }
                    }
                }
            }
        } else {
            map = defaultStyleMap;
        }
        styleMap = map;
        beginToken = tempBeginToken;
        endToken = tempEndToken;
        beginTokenLen = tempBeginToken.length();
        endTokenLen = tempEndToken.length();
    }

    public Map<String, Code[]> getStyleMap() {
        return styleMap;
    }

    private void render(final Ansi ansi, final Code code) {
        if (code.isColor()) {
            if (code.isBackground()) {
                ansi.bg(code.getColor());
            } else {
                ansi.fg(code.getColor());
            }
        } else if (code.isAttribute()) {
            ansi.a(code.getAttribute());
        }
    }

    private void render(final Ansi ansi, final Code... codes) {
        for (final Code code : codes) {
            render(ansi, code);
        }
    }

    /**
     * Renders the given text with the given names which can be ANSI code names or Log4j style names.
     *
     * @param text
     *            The text to render
     * @param names
     *            ANSI code names or Log4j style names.
     * @return A rendered string containing ANSI codes.
     */
    private String render(final String text, final String... names) {
        final Ansi ansi = Ansi.ansi();
        for (final String name : names) {
            final Code[] codes = styleMap.get(name);
            if (codes != null) {
                render(ansi, codes);
            } else {
                render(ansi, toCode(name));
            }
        }
        return ansi.a(text).reset().toString();
    }

    // EXACT COPY OF StringBuilder version of the method but typed as String for input
    @Override
    public void render(final String input, final StringBuilder output, final String styleName)
            throws IllegalArgumentException {
        output.append(render(input, styleName));
    }

    @Override
    public void render(final StringBuilder input, final StringBuilder output) throws IllegalArgumentException {
        int i = 0;
        int j, k;

        while (true) {
            j = input.indexOf(beginToken, i);
            if (j == -1) {
                if (i == 0) {
                    output.append(input);
                    return;
                }
                output.append(input.substring(i, input.length()));
                return;
            }
            output.append(input.substring(i, j));
            k = input.indexOf(endToken, j);

            if (k == -1) {
                output.append(input);
                return;
            }
            j += beginTokenLen;
            final String spec = input.substring(j, k);

            final String[] items = spec.split(AnsiRenderer.CODE_TEXT_SEPARATOR, 2);
            if (items.length == 1) {
                output.append(input);
                return;
            }
            final String replacement = render(items[1], items[0].split(","));

            output.append(replacement);

            i = k + endTokenLen;
        }
    }

    private Code toCode(final String name) {
        return Code.valueOf(toRootUpperCase(name));
    }

    @Override
    public String toString() {
        return "JAnsiMessageRenderer [beginToken=" + beginToken + ", beginTokenLen=" + beginTokenLen + ", endToken="
                + endToken + ", endTokenLen=" + endTokenLen + ", styleMap=" + styleMap + "]";
    }
}
