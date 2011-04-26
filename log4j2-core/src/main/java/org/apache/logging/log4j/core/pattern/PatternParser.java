/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.internal.StatusLogger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Most of the work of the {@link org.apache.logging.log4j.core.layout.PatternLayout} class
 * is delegated to the PatternParser class.
 * <p>It is this class that parses conversion patterns and creates
 * a chained list of {@link PatternConverter PatternConverters}.
 */
public final class PatternParser {
    /**
     * Escape character for format specifier.
     */
    private static final char ESCAPE_CHAR = '%';

    /**
     * Literal state.
     */
    private static final int LITERAL_STATE = 0;

    /**
     * In converter name state.
     */
    private static final int CONVERTER_STATE = 1;

    /**
     * Dot state.
     */
    private static final int DOT_STATE = 3;

    /**
     * Min state.
     */
    private static final int MIN_STATE = 4;

    /**
     * Max state.
     */
    private static final int MAX_STATE = 5;

    /**
     * Does pattern process exceptions.
     */
    private boolean handlesExceptions;

    private final Map<String, Class<PatternConverter>> converterRules;

    protected final static Logger logger = StatusLogger.getLogger();


    public PatternParser(String converterKey) {
        this(converterKey, null);
    }

    /**
     * Constructor
     * @param converterKey The key to lookup the converters.
     */
    public PatternParser(String converterKey, Class expected) {
        PluginManager manager = new PluginManager(converterKey, expected);
        manager.collectPlugins();
        Map<String, PluginType> plugins = manager.getPlugins();
        Map<String, Class<PatternConverter>> converters = new HashMap<String, Class<PatternConverter>>();

        for (PluginType type : plugins.values()) {
            try {
                Class<PatternConverter> clazz = type.getPluginClass();
                ConverterKeys keys = clazz.getAnnotation(ConverterKeys.class);
                if (keys != null) {
                    for (String key : keys.value()) {
                        converters.put(key, clazz);
                    }
                }
            } catch (Exception ex) {

            }
        }
        converterRules = converters;
    }


    public List<PatternConverter> parse(String pattern) {
        List<PatternConverter> converters = new ArrayList<PatternConverter>();
        List<FormattingInfo> fields = new ArrayList<FormattingInfo>();

        parse(pattern, converters, fields);

        LogEventPatternConverter[] patternConverters = new LogEventPatternConverter[converters.size()];
        FormattingInfo[] patternFields = new FormattingInfo[converters.size()];

        int i = 0;
        Iterator fieldIter = fields.iterator();

        for (PatternConverter converter : converters) {
            if (converter instanceof LogEventPatternConverter) {
                patternConverters[i] = (LogEventPatternConverter) converter;
                handlesExceptions |= patternConverters[i].handlesThrowable();
            } else {
                patternConverters[i] = new LiteralPatternConverter("");
            }

            if (fieldIter.hasNext()) {
                patternFields[i] = (FormattingInfo) fieldIter.next();
            } else {
                patternFields[i] = FormattingInfo.getDefault();
            }

            i++;
        }
        return converters;
    }

    public boolean handlesExceptions() {
        return handlesExceptions;
    }

    /**
     * Extract the converter identifier found at position i.
     * <p/>
     * After this function returns, the variable i will point to the
     * first char after the end of the converter identifier.
     * <p/>
     * If i points to a char which is not a character acceptable at the
     * start of a unicode identifier, the value null is returned.
     *
     * @param lastChar       last processed character.
     * @param pattern        format string.
     * @param i              current index into pattern format.
     * @param convBuf        buffer to receive conversion specifier.
     * @param currentLiteral literal to be output in case format specifier in unrecognized.
     * @return position in pattern after converter.
     */
    private static int extractConverter(
        char lastChar, final String pattern, int i, final StringBuilder convBuf,
        final StringBuilder currentLiteral) {
        convBuf.setLength(0);

        // When this method is called, lastChar points to the first character of the
        // conversion word. For example:
        // For "%hello"     lastChar = 'h'
        // For "%-5hello"   lastChar = 'h'
        //System.out.println("lastchar is "+lastChar);
        if (!Character.isUnicodeIdentifierStart(lastChar)) {
            return i;
        }

        convBuf.append(lastChar);

        while ((i < pattern.length()) && Character.isUnicodeIdentifierPart(pattern.charAt(i))) {
            convBuf.append(pattern.charAt(i));
            currentLiteral.append(pattern.charAt(i));
            i++;
        }

        return i;
    }

    /**
     * Extract options.
     *
     * @param pattern conversion pattern.
     * @param i       start of options.
     * @param options array to receive extracted options
     * @return position in pattern after options.
     */
    private static int extractOptions(String pattern, int i, List<String> options) {
        while ((i < pattern.length()) && (pattern.charAt(i) == '{')) {
            int end = pattern.indexOf('}', i);

            if (end == -1) {
                break;
            }

            String r = pattern.substring(i + 1, end);
            options.add(r);
            i = end + 1;
        }

        return i;
    }

    /**
     * Parse a format specifier.
     *
     * @param pattern           pattern to parse.
     * @param patternConverters list to receive pattern converters.
     * @param formattingInfos   list to receive field specifiers corresponding to pattern converters.
     */
    public void parse(final String pattern, final List<PatternConverter> patternConverters,
            final List<FormattingInfo> formattingInfos) {
        if (pattern == null) {
            throw new NullPointerException("pattern");
        }

        StringBuilder currentLiteral = new StringBuilder(32);

        int patternLength = pattern.length();
        int state = LITERAL_STATE;
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
                                    patternConverters.add(new LiteralPatternConverter(currentLiteral.toString()));
                                    formattingInfos.add(FormattingInfo.getDefault());
                                }

                                currentLiteral.setLength(0);
                                currentLiteral.append(c); // append %
                                state = CONVERTER_STATE;
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
                            formattingInfo =
                                new FormattingInfo(true, formattingInfo.getMinLength(),
                                    formattingInfo.getMaxLength());

                            break;

                        case '.':
                            state = DOT_STATE;

                            break;

                        default:

                            if ((c >= '0') && (c <= '9')) {
                                formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(), c - '0',
                                        formattingInfo.getMaxLength());
                                state = MIN_STATE;
                            } else {
                                i = finalizeConverter(c, pattern, i, currentLiteral, formattingInfo,
                                        converterRules, patternConverters, formattingInfos);

                                // Next pattern is assumed to be a literal.
                                state = LITERAL_STATE;
                                formattingInfo = FormattingInfo.getDefault();
                                currentLiteral.setLength(0);
                            }
                    } // switch

                    break;

                case MIN_STATE:
                    currentLiteral.append(c);

                    if ((c >= '0') && (c <= '9')) {
                        formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(),
                                (formattingInfo.getMinLength() * 10) + (c - '0'),
                                formattingInfo.getMaxLength());
                    } else if (c == '.') {
                        state = DOT_STATE;
                    } else {
                        i = finalizeConverter(c, pattern, i, currentLiteral, formattingInfo,
                                converterRules, patternConverters, formattingInfos);
                        state = LITERAL_STATE;
                        formattingInfo = FormattingInfo.getDefault();
                        currentLiteral.setLength(0);
                    }

                    break;

                case DOT_STATE:
                    currentLiteral.append(c);

                    if ((c >= '0') && (c <= '9')) {
                        formattingInfo = new FormattingInfo(formattingInfo.isLeftAligned(),
                            formattingInfo.getMinLength(), c - '0');
                        state = MAX_STATE;
                    } else {
                        logger.error("Error occurred in position " + i
                                + ".\n Was expecting digit, instead got char \"" + c + "\".");

                        state = LITERAL_STATE;
                    }

                    break;

                case MAX_STATE:
                    currentLiteral.append(c);

                    if ((c >= '0') && (c <= '9')) {
                        formattingInfo = new FormattingInfo(
                                formattingInfo.isLeftAligned(), formattingInfo.getMinLength(),
                                (formattingInfo.getMaxLength() * 10) + (c - '0'));
                    } else {
                        i = finalizeConverter(c, pattern, i, currentLiteral, formattingInfo,
                                converterRules, patternConverters, formattingInfos);
                        state = LITERAL_STATE;
                        formattingInfo = FormattingInfo.getDefault();
                        currentLiteral.setLength(0);
                    }

                    break;
            } // switch
        }

        // while
        if (currentLiteral.length() != 0) {
            patternConverters.add(new LiteralPatternConverter(currentLiteral.toString()));
            formattingInfos.add(FormattingInfo.getDefault());
        }
    }

    /**
     * Creates a new PatternConverter.
     *
     * @param converterId       converterId.
     * @param currentLiteral    literal to be used if converter is unrecognized or following converter
     *                          if converterId contains extra characters.
     * @param rules             map of stock pattern converters keyed by format specifier.
     * @param options           converter options.
     * @return converter or null.
     */
    private PatternConverter createConverter(final String converterId, final StringBuilder currentLiteral,
            final Map<String, Class<PatternConverter>> rules,
            final List<String> options) {
        String converterName = converterId;
        Class<PatternConverter> converterClass = null;

        for (int i = converterId.length(); (i > 0) && (converterClass == null); i--) {
            converterName = converterName.substring(0, i);

            if ((converterClass == null) && (rules != null)) {
                converterClass = rules.get(converterName);
            }
        }

        if (converterClass == null) {
            logger.error("Unrecognized format specifier [" + converterId + "]");

            return null;
        }

        try {
            Method factory = converterClass.getMethod(
                    "newInstance",
                    new Class[]{
                        Class.forName("[Ljava.lang.String;")
                    });
            String[] optionsArray = new String[options.size()];
            optionsArray = options.toArray(optionsArray);

            Object newObj = factory.invoke(null, new Object[]{optionsArray});

            if (newObj instanceof PatternConverter) {
                currentLiteral.delete(0, currentLiteral.length()
                        - (converterId.length() - converterName.length()));

                return (PatternConverter) newObj;
            } else {
                logger.warn("Class " + converterClass.getName() + " does not extend PatternConverter.");
            }
        } catch (Exception ex) {
            logger.error("Error creating converter for " + converterId, ex);

            try {
                //
                //  try default constructor
                PatternConverter pc = converterClass.newInstance();
                currentLiteral.delete(0, currentLiteral.length()
                        - (converterId.length() - converterName.length()));

                return pc;
            } catch (Exception ex2) {
                logger.error("Error creating converter for " + converterId, ex2);
            }
        }

        return null;
    }

    /**
     * Processes a format specifier sequence.
     *
     * @param c                 initial character of format specifier.
     * @param pattern           conversion pattern
     * @param i                 current position in conversion pattern.
     * @param currentLiteral    current literal.
     * @param formattingInfo    current field specifier.
     * @param rules             map of stock pattern converters keyed by format specifier.
     * @param patternConverters list to receive parsed pattern converter.
     * @param formattingInfos   list to receive corresponding field specifier.
     * @return position after format specifier sequence.
     */
    private int finalizeConverter(char c, String pattern, int i,
            final StringBuilder currentLiteral, final FormattingInfo formattingInfo,
            final Map<String, Class<PatternConverter>> rules,
            final List<PatternConverter> patternConverters, final List<FormattingInfo> formattingInfos) {
        StringBuilder convBuf = new StringBuilder();
        i = extractConverter(c, pattern, i, convBuf, currentLiteral);

        String converterId = convBuf.toString();

        List<String> options = new ArrayList<String>();
        i = extractOptions(pattern, i, options);

        PatternConverter pc = createConverter(converterId, currentLiteral, rules, options);

        if (pc == null) {
            StringBuilder msg;

            if ((converterId == null) || (converterId.length() == 0)) {
                msg =
                    new StringBuilder("Empty conversion specifier starting at position ");
            } else {
                msg = new StringBuilder("Unrecognized conversion specifier [");
                msg.append(converterId);
                msg.append("] starting at position ");
            }

            msg.append(Integer.toString(i));
            msg.append(" in conversion pattern.");

            logger.error(msg.toString());

            patternConverters.add(new LiteralPatternConverter(currentLiteral.toString()));
            formattingInfos.add(FormattingInfo.getDefault());
        } else {
            patternConverters.add(pc);
            formattingInfos.add(formattingInfo);

            if (currentLiteral.length() > 0) {
                patternConverters.add(new LiteralPatternConverter(currentLiteral.toString()));
                formattingInfos.add(FormattingInfo.getDefault());
            }
        }

        currentLiteral.setLength(0);

        return i;
    }
}
