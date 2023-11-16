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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.util.SystemNanoClock;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Most of the work of the {@link org.apache.logging.log4j.core.layout.PatternLayout} class is delegated to the
 * PatternParser class.
 * <p>
 * It is this class that parses conversion patterns and creates a chained list of {@link PatternConverter
 * PatternConverters}.
 */
public final class PatternParser {
    static final String DISABLE_ANSI = "disableAnsi";
    static final String NO_CONSOLE_NO_ANSI = "noConsoleNoAnsi";

    /**
     * Escape character for format specifier.
     */
    private static final char ESCAPE_CHAR = '%';

    /**
     * The states the parser can be in while parsing the pattern.
     */
    private enum ParserState {
        /**
         * Literal state.
         */
        LITERAL_STATE,

        /**
         * In converter name state.
         */
        CONVERTER_STATE,

        /**
         * Dot state.
         */
        DOT_STATE,

        /**
         * Min state.
         */
        MIN_STATE,

        /**
         * Max state.
         */
        MAX_STATE;
    }

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final int BUF_SIZE = 32;

    private static final int DECIMAL = 10;

    private final Configuration config;

    private final Map<String, Class<PatternConverter>> converterRules;

    /**
     * Constructor.
     *
     * @param converterKey
     *            The type of converters that will be used.
     */
    public PatternParser(final String converterKey) {
        this(null, converterKey, null, null);
    }

    /**
     * Constructor.
     *
     * @param config
     *            The current Configuration or {@code null}.
     * @param converterKey
     *            The key to lookup the converters.
     * @param expected
     *            The expected base Class of each Converter or {@code null}.
     */
    public PatternParser(final Configuration config, final String converterKey, final Class<?> expected) {
        this(config, converterKey, expected, null);
    }

    /**
     * Constructor.
     *
     * @param config
     *            The current Configuration or {@code null}.
     * @param converterKey
     *            The key to lookup the converters.
     * @param expectedClass
     *            The expected base Class of each Converter or {@code null}.
     * @param filterClass
     *            Filter the returned plugins after calling the plugin manager, can be {@code null}.
     */
    public PatternParser(
            final Configuration config,
            final String converterKey,
            final Class<?> expectedClass,
            final Class<?> filterClass) {
        this.config = config;
        final PluginManager manager = new PluginManager(converterKey);
        manager.collectPlugins(config == null ? null : config.getPluginPackages());
        final Map<String, PluginType<?>> plugins = manager.getPlugins();
        final Map<String, Class<PatternConverter>> converters = new LinkedHashMap<>();

        for (final PluginType<?> type : plugins.values()) {
            try {
                @SuppressWarnings("unchecked")
                final Class<PatternConverter> clazz = (Class<PatternConverter>) type.getPluginClass();
                if (filterClass != null && !filterClass.isAssignableFrom(clazz)) {
                    continue;
                }
                final ConverterKeys keys = clazz.getAnnotation(ConverterKeys.class);
                if (keys != null) {
                    for (final String key : keys.value()) {
                        if (converters.containsKey(key)) {
                            LOGGER.warn(
                                    "Converter key '{}' is already mapped to '{}'. "
                                            + "Sorry, Dave, I can't let you do that! Ignoring plugin [{}].",
                                    key,
                                    converters.get(key),
                                    clazz);
                        } else {
                            converters.put(key, clazz);
                        }
                    }
                }
            } catch (final Exception ex) {
                LOGGER.error("Error processing plugin " + type.getElementName(), ex);
            }
        }
        converterRules = converters;
    }

    public List<PatternFormatter> parse(final String pattern) {
        return parse(pattern, false, false, false);
    }

    public List<PatternFormatter> parse(
            final String pattern, final boolean alwaysWriteExceptions, final boolean noConsoleNoAnsi) {
        return parse(pattern, alwaysWriteExceptions, false, noConsoleNoAnsi);
    }

    public List<PatternFormatter> parse(
            final String pattern,
            final boolean alwaysWriteExceptions,
            final boolean disableAnsi,
            final boolean noConsoleNoAnsi) {
        final List<PatternFormatter> list = new ArrayList<>();
        final List<PatternConverter> converters = new ArrayList<>();
        final List<FormattingInfo> fields = new ArrayList<>();

        parse(pattern, converters, fields, disableAnsi, noConsoleNoAnsi, true);

        final Iterator<FormattingInfo> fieldIter = fields.iterator();
        boolean handlesThrowable = false;

        for (final PatternConverter converter : converters) {
            if (converter instanceof NanoTimePatternConverter) {
                // LOG4J2-1074 Switch to actual clock if nanosecond timestamps are required in config.
                // LOG4J2-1248 set config nanoclock
                if (config != null) {
                    config.setNanoClock(new SystemNanoClock());
                }
            }
            LogEventPatternConverter pc;
            if (converter instanceof LogEventPatternConverter) {
                pc = (LogEventPatternConverter) converter;
                handlesThrowable |= pc.handlesThrowable();
            } else {
                pc = SimpleLiteralPatternConverter.of(Strings.EMPTY);
            }

            FormattingInfo field;
            if (fieldIter.hasNext()) {
                field = fieldIter.next();
            } else {
                field = FormattingInfo.getDefault();
            }
            list.add(new PatternFormatter(pc, field));
        }
        if (alwaysWriteExceptions && !handlesThrowable) {
            final LogEventPatternConverter pc = ExtendedThrowablePatternConverter.newInstance(config, null);
            list.add(new PatternFormatter(pc, FormattingInfo.getDefault()));
        }
        return list;
    }

    /**
     * Extracts the converter identifier found at the given start position.
     *
     * @param lastChar
     *        last processed character.
     * @param pattern
     *        format string.
     * @param start
     *        current index into pattern format.
     * @param convBuf
     *        buffer to receive conversion specifier.
     * @param currentLiteral
     *        literal to be output in case format specifier in unrecognized.
     * @return position in pattern after converter.
     */
    private static int extractConverter(
            final char lastChar,
            final String pattern,
            final int start,
            final StringBuilder convBuf,
            final StringBuilder currentLiteral) {
        int i = start;
        convBuf.setLength(0);

        // When this method is called, lastChar points to the first character of the
        // conversion word. For example:
        // For "%hello" lastChar = 'h'
        // For "%-5hello" lastChar = 'h'
        // System.out.println("lastchar is "+lastChar);
        if (!Character.isUnicodeIdentifierStart(lastChar)) {
            return i;
        }

        convBuf.append(lastChar);

        while (i < pattern.length() && Character.isUnicodeIdentifierPart(pattern.charAt(i))) {
            convBuf.append(pattern.charAt(i));
            currentLiteral.append(pattern.charAt(i));
            i++;
        }

        return i;
    }

    /**
     * Extract options.
     *
     * @param pattern
     *            conversion pattern.
     * @param start
     *            start of options.
     * @param options
     *            array to receive extracted options
     * @return position in pattern after options.
     */
    private static int extractOptions(final String pattern, final int start, final List<String> options) {
        int i = start;
        while (i < pattern.length() && pattern.charAt(i) == '{') {
            i++; // skip opening "{"
            final int begin = i; // position of first real char
            int depth = 1; // already inside one level
            while (depth > 0 && i < pattern.length()) {
                final char c = pattern.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    // TODO(?) maybe escaping of { and } with \ or %
                }
                i++;
            } // while

            if (depth > 0) { // option not closed, continue with pattern after closing bracket
                i = pattern.lastIndexOf('}');
                if (i == -1 || i < start) {
                    // if no closing bracket could be found or there is no closing bracket behind the starting
                    // character of our parsing process continue parsing after the first opening bracket
                    return begin;
                }
                return i + 1;
            }

            options.add(pattern.substring(begin, i - 1));
        } // while

        return i;
    }

    /**
     * Parse a format specifier.
     *
     * @param pattern
     *            pattern to parse.
     * @param patternConverters
     *            list to receive pattern converters.
     * @param formattingInfos
     *            list to receive field specifiers corresponding to pattern converters.
     * @param noConsoleNoAnsi
     *            do not do not output ANSI escape codes if {@link System#console()}
     * @param convertBackslashes if {@code true}, backslash characters are treated as escape characters and character
     *            sequences like "\" followed by "t" (backslash+t) are converted to special characters like '\t' (tab).
     */
    public void parse(
            final String pattern,
            final List<PatternConverter> patternConverters,
            final List<FormattingInfo> formattingInfos,
            final boolean noConsoleNoAnsi,
            final boolean convertBackslashes) {
        parse(pattern, patternConverters, formattingInfos, false, noConsoleNoAnsi, convertBackslashes);
    }

    /**
     * Parse a format specifier.
     *
     * @param pattern
     *            pattern to parse.
     * @param patternConverters
     *            list to receive pattern converters.
     * @param formattingInfos
     *            list to receive field specifiers corresponding to pattern converters.
     * @param disableAnsi
     *            do not output ANSI escape codes
     * @param noConsoleNoAnsi
     *            do not do not output ANSI escape codes if {@link System#console()}
     * @param convertBackslashes if {@code true}, backslash characters are treated as escape characters and character
     *            sequences like "\" followed by "t" (backslash+t) are converted to special characters like '\t' (tab).
     */
    public void parse(
            final String pattern,
            final List<PatternConverter> patternConverters,
            final List<FormattingInfo> formattingInfos,
            final boolean disableAnsi,
            final boolean noConsoleNoAnsi,
            final boolean convertBackslashes) {
        Objects.requireNonNull(pattern, "pattern");

        final StringBuilder currentLiteral = new StringBuilder(BUF_SIZE);

        final int patternLength = pattern.length();
        ParserState state = ParserState.LITERAL_STATE;
        char c;
        int i = 0;
        FormattingInfo formattingInfo = FormattingInfo.getDefault();

        while (i < patternLength) {
            c = pattern.charAt(i++);

            switch (state) {
                case LITERAL_STATE:

                    // In literal state, the last char is always a literal.
                    if (i == patternLength) {
                        currentLiteral.append(c);

                        continue;
                    }

                    if (c == ESCAPE_CHAR) {
                        // peek at the next char.
                        switch (pattern.charAt(i)) {
                            case ESCAPE_CHAR:
                                currentLiteral.append(c);
                                i++; // move pointer

                                break;

                            default:
                                if (currentLiteral.length() != 0) {
                                    patternConverters.add(
                                            literalPattern(currentLiteral.toString(), convertBackslashes));
                                    formattingInfos.add(FormattingInfo.getDefault());
                                }

                                currentLiteral.setLength(0);
                                currentLiteral.append(c); // append %
                                state = ParserState.CONVERTER_STATE;
                                formattingInfo = FormattingInfo.getDefault();
                        }
                    } else {
                        currentLiteral.append(c);
                    }

                    break;

                case CONVERTER_STATE:
                    currentLiteral.append(c);

                    switch (c) {
                        case '0':
                            // a '0' directly after the % sign indicates zero-padding
                            formattingInfo = new FormattingInfo(
                                    formattingInfo.isLeftAligned(),
                                    formattingInfo.getMinLength(),
                                    formattingInfo.getMaxLength(),
                                    formattingInfo.isLeftTruncate(),
                                    true);
                            break;

                        case '-':
                            formattingInfo = new FormattingInfo(
                                    true,
                                    formattingInfo.getMinLength(),
                                    formattingInfo.getMaxLength(),
                                    formattingInfo.isLeftTruncate(),
                                    formattingInfo.isZeroPad());
                            break;

                        case '.':
                            state = ParserState.DOT_STATE;
                            break;

                        default:
                            if (c >= '0' && c <= '9') {
                                formattingInfo = new FormattingInfo(
                                        formattingInfo.isLeftAligned(),
                                        c - '0',
                                        formattingInfo.getMaxLength(),
                                        formattingInfo.isLeftTruncate(),
                                        formattingInfo.isZeroPad());
                                state = ParserState.MIN_STATE;
                            } else {
                                i = finalizeConverter(
                                        c,
                                        pattern,
                                        i,
                                        currentLiteral,
                                        formattingInfo,
                                        converterRules,
                                        patternConverters,
                                        formattingInfos,
                                        disableAnsi,
                                        noConsoleNoAnsi,
                                        convertBackslashes);

                                // Next pattern is assumed to be a literal.
                                state = ParserState.LITERAL_STATE;
                                formattingInfo = FormattingInfo.getDefault();
                                currentLiteral.setLength(0);
                            }
                    } // switch

                    break;

                case MIN_STATE:
                    currentLiteral.append(c);

                    if (c >= '0' && c <= '9') {
                        // Multiply the existing value and add the value of the number just encountered.
                        formattingInfo = new FormattingInfo(
                                formattingInfo.isLeftAligned(),
                                formattingInfo.getMinLength() * DECIMAL + c - '0',
                                formattingInfo.getMaxLength(),
                                formattingInfo.isLeftTruncate(),
                                formattingInfo.isZeroPad());
                    } else if (c == '.') {
                        state = ParserState.DOT_STATE;
                    } else {
                        i = finalizeConverter(
                                c,
                                pattern,
                                i,
                                currentLiteral,
                                formattingInfo,
                                converterRules,
                                patternConverters,
                                formattingInfos,
                                disableAnsi,
                                noConsoleNoAnsi,
                                convertBackslashes);
                        state = ParserState.LITERAL_STATE;
                        formattingInfo = FormattingInfo.getDefault();
                        currentLiteral.setLength(0);
                    }

                    break;

                case DOT_STATE:
                    currentLiteral.append(c);
                    switch (c) {
                        case '-':
                            formattingInfo = new FormattingInfo(
                                    formattingInfo.isLeftAligned(),
                                    formattingInfo.getMinLength(),
                                    formattingInfo.getMaxLength(),
                                    false,
                                    formattingInfo.isZeroPad());
                            break;

                        default:
                            if (c >= '0' && c <= '9') {
                                formattingInfo = new FormattingInfo(
                                        formattingInfo.isLeftAligned(),
                                        formattingInfo.getMinLength(),
                                        c - '0',
                                        formattingInfo.isLeftTruncate(),
                                        formattingInfo.isZeroPad());
                                state = ParserState.MAX_STATE;
                            } else {
                                LOGGER.error("Error occurred in position " + i
                                        + ".\n Was expecting digit, instead got char \"" + c + "\".");

                                state = ParserState.LITERAL_STATE;
                            }
                    }

                    break;

                case MAX_STATE:
                    currentLiteral.append(c);

                    if (c >= '0' && c <= '9') {
                        // Multiply the existing value and add the value of the number just encountered.
                        formattingInfo = new FormattingInfo(
                                formattingInfo.isLeftAligned(),
                                formattingInfo.getMinLength(),
                                formattingInfo.getMaxLength() * DECIMAL + c - '0',
                                formattingInfo.isLeftTruncate(),
                                formattingInfo.isZeroPad());
                    } else {
                        i = finalizeConverter(
                                c,
                                pattern,
                                i,
                                currentLiteral,
                                formattingInfo,
                                converterRules,
                                patternConverters,
                                formattingInfos,
                                disableAnsi,
                                noConsoleNoAnsi,
                                convertBackslashes);
                        state = ParserState.LITERAL_STATE;
                        formattingInfo = FormattingInfo.getDefault();
                        currentLiteral.setLength(0);
                    }

                    break;
            } // switch
        }

        // while
        if (currentLiteral.length() != 0) {
            patternConverters.add(literalPattern(currentLiteral.toString(), convertBackslashes));
            formattingInfos.add(FormattingInfo.getDefault());
        }
    }

    /**
     * Creates a new PatternConverter.
     *
     * @param converterId
     *            converterId.
     * @param currentLiteral
     *            literal to be used if converter is unrecognized or following converter if converterId contains extra
     *            characters.
     * @param rules
     *            map of stock pattern converters keyed by format specifier.
     * @param options
     *            converter options.
     * @param disableAnsi
     *            do not output ANSI escape codes
     * @param noConsoleNoAnsi
     *            do not do not output ANSI escape codes if {@link System#console()}
     * @return converter or null.
     */
    private PatternConverter createConverter(
            final String converterId,
            final StringBuilder currentLiteral,
            final Map<String, Class<PatternConverter>> rules,
            final List<String> options,
            final boolean disableAnsi,
            final boolean noConsoleNoAnsi) {
        String converterName = converterId;
        Class<PatternConverter> converterClass = null;

        if (rules == null) {
            LOGGER.error("Null rules for [" + converterId + ']');
            return null;
        }
        for (int i = converterId.length(); i > 0 && converterClass == null; i--) {
            converterName = converterName.substring(0, i);
            converterClass = rules.get(converterName);
        }

        if (converterClass == null) {
            LOGGER.error("Unrecognized format specifier [" + converterId + ']');
            return null;
        }

        if (AnsiConverter.class.isAssignableFrom(converterClass)) {
            options.add(DISABLE_ANSI + '=' + disableAnsi);
            options.add(NO_CONSOLE_NO_ANSI + '=' + noConsoleNoAnsi);
        }
        // Work around the regression bug in Class.getDeclaredMethods() in Oracle Java in version > 1.6.0_17:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6815786
        final Method[] methods = converterClass.getDeclaredMethods();
        Method newInstanceMethod = null;
        for (final Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.getDeclaringClass().equals(converterClass)
                    && method.getName().equals("newInstance")
                    && areValidNewInstanceParameters(method.getParameterTypes())) {
                if (newInstanceMethod == null) {
                    newInstanceMethod = method;
                } else if (method.getReturnType().equals(newInstanceMethod.getReturnType())) {
                    LOGGER.error("Class " + converterClass + " cannot contain multiple static newInstance methods");
                    return null;
                }
            }
        }
        if (newInstanceMethod == null) {
            LOGGER.error("Class " + converterClass + " does not contain a static newInstance method");
            return null;
        }

        final Class<?>[] parmTypes = newInstanceMethod.getParameterTypes();
        final Object[] parms = parmTypes.length > 0 ? new Object[parmTypes.length] : null;

        if (parms != null) {
            int i = 0;
            boolean errors = false;
            for (final Class<?> clazz : parmTypes) {
                if (clazz.isArray() && clazz.getName().equals("[Ljava.lang.String;")) {
                    final String[] optionsArray = options.toArray(Strings.EMPTY_ARRAY);
                    parms[i] = optionsArray;
                } else if (clazz.isAssignableFrom(Configuration.class)) {
                    parms[i] = config;
                } else {
                    LOGGER.error("Unknown parameter type " + clazz.getName() + " for static newInstance method of "
                            + converterClass.getName());
                    errors = true;
                }
                ++i;
            }
            if (errors) {
                return null;
            }
        }

        try {
            final Object newObj = newInstanceMethod.invoke(null, parms);

            if (newObj instanceof PatternConverter) {
                currentLiteral.delete(0, currentLiteral.length() - (converterId.length() - converterName.length()));

                return (PatternConverter) newObj;
            }
            LOGGER.warn("Class {} does not extend PatternConverter.", converterClass.getName());
        } catch (final Exception ex) {
            LOGGER.error("Error creating converter for " + converterId, ex);
        }

        return null;
    }

    /** LOG4J2-2564: Returns true if all method parameters are valid for injection. */
    private static boolean areValidNewInstanceParameters(final Class<?>[] parameterTypes) {
        for (Class<?> clazz : parameterTypes) {
            if (!clazz.isAssignableFrom(Configuration.class)
                    && !(clazz.isArray() && "[Ljava.lang.String;".equals(clazz.getName()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Processes a format specifier sequence.
     *
     * @param c
     *            initial character of format specifier.
     * @param pattern
     *            conversion pattern
     * @param start
     *            current position in conversion pattern.
     * @param currentLiteral
     *            current literal.
     * @param formattingInfo
     *            current field specifier.
     * @param rules
     *            map of stock pattern converters keyed by format specifier.
     * @param patternConverters
     *            list to receive parsed pattern converter.
     * @param formattingInfos
     *            list to receive corresponding field specifier.
     * @param disableAnsi
     *            do not output ANSI escape codes
     * @param noConsoleNoAnsi
     *            do not do not output ANSI escape codes if {@link System#console()}
     * @param convertBackslashes if {@code true}, backslash characters are treated as escape characters and character
     *            sequences like "\" followed by "t" (backslash+t) are converted to special characters like '\t' (tab).
     * @return position after format specifier sequence.
     */
    private int finalizeConverter(
            final char c,
            final String pattern,
            final int start,
            final StringBuilder currentLiteral,
            final FormattingInfo formattingInfo,
            final Map<String, Class<PatternConverter>> rules,
            final List<PatternConverter> patternConverters,
            final List<FormattingInfo> formattingInfos,
            final boolean disableAnsi,
            final boolean noConsoleNoAnsi,
            final boolean convertBackslashes) {
        int i = start;
        final StringBuilder convBuf = new StringBuilder();
        i = extractConverter(c, pattern, i, convBuf, currentLiteral);

        final String converterId = convBuf.toString();

        final List<String> options = new ArrayList<>();
        i = extractOptions(pattern, i, options);

        final PatternConverter pc =
                createConverter(converterId, currentLiteral, rules, options, disableAnsi, noConsoleNoAnsi);

        if (pc == null) {
            StringBuilder msg;

            if (Strings.isEmpty(converterId)) {
                msg = new StringBuilder("Empty conversion specifier starting at position ");
            } else {
                msg = new StringBuilder("Unrecognized conversion specifier [");
                msg.append(converterId);
                msg.append("] starting at position ");
            }

            msg.append(i);
            msg.append(" in conversion pattern.");

            LOGGER.error(msg.toString());

            patternConverters.add(literalPattern(currentLiteral.toString(), convertBackslashes));
            formattingInfos.add(FormattingInfo.getDefault());
        } else {
            patternConverters.add(pc);
            formattingInfos.add(formattingInfo);

            if (currentLiteral.length() > 0) {
                patternConverters.add(literalPattern(currentLiteral.toString(), convertBackslashes));
                formattingInfos.add(FormattingInfo.getDefault());
            }
        }

        currentLiteral.setLength(0);

        return i;
    }

    // Create a literal pattern converter with support for substitutions if necessary
    private LogEventPatternConverter literalPattern(final String literal, final boolean convertBackslashes) {
        if (config != null && LiteralPatternConverter.containsSubstitutionSequence(literal)) {
            return new LiteralPatternConverter(config, literal, convertBackslashes);
        }
        return SimpleLiteralPatternConverter.of(literal, convertBackslashes);
    }
}
