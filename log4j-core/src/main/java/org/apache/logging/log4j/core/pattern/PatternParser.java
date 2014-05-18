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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
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
     *            The current Configuration.
     * @param converterKey
     *            The key to lookup the converters.
     * @param expected
     *            The expected base Class of each Converter.
     */
    public PatternParser(final Configuration config, final String converterKey, final Class<?> expected) {
        this(config, converterKey, expected, null);
    }

    /**
     * Constructor.
     *
     * @param config
     *            The current Configuration.
     * @param converterKey
     *            The key to lookup the converters.
     * @param expectedClass
     *            The expected base Class of each Converter.
     * @param filterClass
     *            Filter the returned plugins after calling the plugin manager.
     */
    public PatternParser(final Configuration config, final String converterKey, final Class<?> expectedClass,
            final Class<?> filterClass) {
        this.config = config;
        final PluginManager manager = new PluginManager(converterKey, expectedClass);
        manager.collectPlugins();
        final Map<String, PluginType<?>> plugins = manager.getPlugins();
        final Map<String, Class<PatternConverter>> converters = new HashMap<String, Class<PatternConverter>>();

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
                        converters.put(key, clazz);
                    }
                }
            } catch (final Exception ex) {
                LOGGER.error("Error processing plugin " + type.getElementName(), ex);
            }
        }
        converterRules = converters;
    }

    public List<PatternFormatter> parse(final String pattern) {
        return parse(pattern, false, false);
    }

    public List<PatternFormatter> parse(final String pattern, final boolean alwaysWriteExceptions,
            boolean noConsoleNoAnsi) {
        final List<PatternFormatter> list = new ArrayList<PatternFormatter>();
        final List<PatternConverter> converters = new ArrayList<PatternConverter>();
        final List<FormattingInfo> fields = new ArrayList<FormattingInfo>();

        parse(pattern, converters, fields, noConsoleNoAnsi);

        final Iterator<FormattingInfo> fieldIter = fields.iterator();
        boolean handlesThrowable = false;

        for (final PatternConverter converter : converters) {
            LogEventPatternConverter pc;
            if (converter instanceof LogEventPatternConverter) {
                pc = (LogEventPatternConverter) converter;
                handlesThrowable |= pc.handlesThrowable();
            } else {
                pc = new LiteralPatternConverter(config, Strings.EMPTY);
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
            final LogEventPatternConverter pc = ExtendedThrowablePatternConverter.newInstance(null);
            list.add(new PatternFormatter(pc, FormattingInfo.getDefault()));
        }
        return list;
    }

    /**
     * Extract the converter identifier found at position i.
     * <p/>
     * After this function returns, the variable i will point to the first char after the end of the converter
     * identifier.
     * <p/>
     * If i points to a char which is not a character acceptable at the start of a unicode identifier, the value null is
     * returned.
     *
     * @param lastChar
     *            last processed character.
     * @param pattern
     *            format string.
     * @param i
     *            current index into pattern format.
     * @param convBuf
     *            buffer to receive conversion specifier.
     * @param currentLiteral
     *            literal to be output in case format specifier in unrecognized.
     * @return position in pattern after converter.
     */
    private static int extractConverter(final char lastChar, final String pattern, int i, final StringBuilder convBuf,
            final StringBuilder currentLiteral) {
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
     * @param i
     *            start of options.
     * @param options
     *            array to receive extracted options
     * @return position in pattern after options.
     */
    private static int extractOptions(final String pattern, int i, final List<String> options) {
        while (i < pattern.length() && pattern.charAt(i) == '{') {
            final int begin = i++;
            int end;
            int depth = 0;
            do {
                end = pattern.indexOf('}', i);
                if (end != -1) {
                    final int next = pattern.indexOf("{", i);
                    if (next != -1 && next < end) {
                        i = end + 1;
                        ++depth;
                    } else if (depth > 0) {
                        --depth;
                    }
                }
            } while (depth > 0);

            if (end == -1) {
                break;
            }

            final String r = pattern.substring(begin + 1, end);
            options.add(r);
            i = end + 1;
        }

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
     *            TODO
     */
    public void parse(final String pattern, final List<PatternConverter> patternConverters,
            final List<FormattingInfo> formattingInfos, final boolean noConsoleNoAnsi) {
        if (pattern == null) {
            throw new NullPointerException("pattern");
        }

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
                            patternConverters.add(new LiteralPatternConverter(config, currentLiteral.toString()));
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
                case '-':
                    formattingInfo = new FormattingInfo(true, formattingInfo.getMinLength(),
                            formattingInfo.getMaxLength());
                    break;

                case '.':
                    state = ParserState.DOT_STATE;
                    break;

                default:

                    if (c >= '0' && c <= '9') {
                        formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), c - '0',
                                formattingInfo.getMaxLength());
                        state = ParserState.MIN_STATE;
                    } else {
                        i = finalizeConverter(c, pattern, i, currentLiteral, formattingInfo, converterRules,
                                patternConverters, formattingInfos, noConsoleNoAnsi);

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
                    formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), formattingInfo.getMinLength()
                            * DECIMAL + c - '0', formattingInfo.getMaxLength());
                } else if (c == '.') {
                    state = ParserState.DOT_STATE;
                } else {
                    i = finalizeConverter(c, pattern, i, currentLiteral, formattingInfo, converterRules,
                            patternConverters, formattingInfos, noConsoleNoAnsi);
                    state = ParserState.LITERAL_STATE;
                    formattingInfo = FormattingInfo.getDefault();
                    currentLiteral.setLength(0);
                }

                break;

            case DOT_STATE:
                currentLiteral.append(c);

                if (c >= '0' && c <= '9') {
                    formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), formattingInfo.getMinLength(),
                            c - '0');
                    state = ParserState.MAX_STATE;
                } else {
                    LOGGER.error("Error occurred in position " + i + ".\n Was expecting digit, instead got char \"" + c
                            + "\".");

                    state = ParserState.LITERAL_STATE;
                }

                break;

            case MAX_STATE:
                currentLiteral.append(c);

                if (c >= '0' && c <= '9') {
                    // Multiply the existing value and add the value of the number just encountered.
                    formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), formattingInfo.getMinLength(),
                            formattingInfo.getMaxLength() * DECIMAL + c - '0');
                } else {
                    i = finalizeConverter(c, pattern, i, currentLiteral, formattingInfo, converterRules,
                            patternConverters, formattingInfos, noConsoleNoAnsi);
                    state = ParserState.LITERAL_STATE;
                    formattingInfo = FormattingInfo.getDefault();
                    currentLiteral.setLength(0);
                }

                break;
            } // switch
        }

        // while
        if (currentLiteral.length() != 0) {
            patternConverters.add(new LiteralPatternConverter(config, currentLiteral.toString()));
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
     * @param noConsoleNoAnsi TODO
     * @return converter or null.
     */
    private PatternConverter createConverter(final String converterId, final StringBuilder currentLiteral,
            final Map<String, Class<PatternConverter>> rules, final List<String> options, boolean noConsoleNoAnsi) {
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
            options.add(NO_CONSOLE_NO_ANSI + '=' + noConsoleNoAnsi);
        }
        // Work around the regression bug in Class.getDeclaredMethods() in Oracle Java in version > 1.6.0_17:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6815786
        final Method[] methods = converterClass.getDeclaredMethods();
        Method newInstanceMethod = null;
        for (final Method method : methods) {
            if (Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass().equals(converterClass)
                    && method.getName().equals("newInstance")) {
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
                    final String[] optionsArray = options.toArray(new String[options.size()]);
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
            } else {
                LOGGER.warn("Class " + converterClass.getName() + " does not extend PatternConverter.");
            }
        } catch (final Exception ex) {
            LOGGER.error("Error creating converter for " + converterId, ex);
        }

        return null;
    }

    /**
     * Processes a format specifier sequence.
     *
     * @param c
     *            initial character of format specifier.
     * @param pattern
     *            conversion pattern
     * @param i
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
     * @param noConsoleNoAnsi
     *            TODO
     * @return position after format specifier sequence.
     */
    private int finalizeConverter(final char c, final String pattern, int i, final StringBuilder currentLiteral,
            final FormattingInfo formattingInfo, final Map<String, Class<PatternConverter>> rules,
            final List<PatternConverter> patternConverters, final List<FormattingInfo> formattingInfos,
            final boolean noConsoleNoAnsi) {
        final StringBuilder convBuf = new StringBuilder();
        i = extractConverter(c, pattern, i, convBuf, currentLiteral);

        final String converterId = convBuf.toString();

        final List<String> options = new ArrayList<String>();
        i = extractOptions(pattern, i, options);

        final PatternConverter pc = createConverter(converterId, currentLiteral, rules, options, noConsoleNoAnsi);

        if (pc == null) {
            StringBuilder msg;

            if (Strings.isEmpty(converterId)) {
                msg = new StringBuilder("Empty conversion specifier starting at position ");
            } else {
                msg = new StringBuilder("Unrecognized conversion specifier [");
                msg.append(converterId);
                msg.append("] starting at position ");
            }

            msg.append(Integer.toString(i));
            msg.append(" in conversion pattern.");

            LOGGER.error(msg.toString());

            patternConverters.add(new LiteralPatternConverter(config, currentLiteral.toString()));
            formattingInfos.add(FormattingInfo.getDefault());
        } else {
            patternConverters.add(pc);
            formattingInfos.add(formattingInfo);

            if (currentLiteral.length() > 0) {
                patternConverters.add(new LiteralPatternConverter(config, currentLiteral.toString()));
                formattingInfos.add(FormattingInfo.getDefault());
            }
        }

        currentLiteral.setLength(0);

        return i;
    }
}
