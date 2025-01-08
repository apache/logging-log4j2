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
package org.apache.logging.log4j.core.filter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFormatMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;

/**
 * A filter that matches the given regular expression pattern against messages.
 */
@Plugin(name = "RegexFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public final class RegexFilter extends AbstractFilter {

    private static final int DEFAULT_PATTERN_FLAGS = 0;
    private final Pattern pattern;
    private final boolean useRawMessage;

    private RegexFilter(final boolean raw, final Pattern pattern, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.pattern = pattern;
        this.useRawMessage = raw;
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        if (useRawMessage || params == null || params.length == 0) {
            return filter(msg);
        }
        return filter(ParameterizedMessage.format(msg, params));
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        if (msg == null) {
            return onMismatch;
        }
        return filter(msg.toString());
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        if (msg == null) {
            return onMismatch;
        }
        final String text = targetMessageTest(msg);
        return filter(text);
    }

    @Override
    public Result filter(final LogEvent event) {
        final String text = targetMessageTest(event.getMessage());
        return filter(text);
    }

    // While `Message#getFormat()` is broken in general, it still makes sense for certain types.
    // Hence, suppress the deprecation warning.
    @SuppressWarnings("deprecation")
    private String targetMessageTest(final Message message) {
        return useRawMessage
                        && (message instanceof ParameterizedMessage
                                || message instanceof StringFormattedMessage
                                || message instanceof MessageFormatMessage
                                || message instanceof StructuredDataMessage)
                ? message.getFormat()
                : message.getFormattedMessage();
    }

    private Result filter(final String msg) {
        if (msg == null) {
            return onMismatch;
        }
        final Matcher m = pattern.matcher(msg);
        return m.matches() ? onMatch : onMismatch;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("useRaw=").append(useRawMessage);
        sb.append(", pattern=").append(pattern.toString());
        return sb.toString();
    }

    /**
     * Creates a Filter that matches a regular expression.
     *
     * @param regex
     *        The regular expression to match.
     * @param patternFlags
     *        An array of Strings where each String is a {@link Pattern#compile(String, int)} compilation flag.
     * @param useRawMsg
     *        If {@code true}, for {@link ParameterizedMessage}, {@link StringFormattedMessage}, and {@link MessageFormatMessage}, the message format pattern; for {@link StructuredDataMessage}, the message field will be used as the match target.
     * @param match
     *        The action to perform when a match occurs.
     * @param mismatch
     *        The action to perform when a mismatch occurs.
     * @return The RegexFilter.
     * @throws IllegalAccessException  When there is no access to the definition of the specified member.
     * @throws IllegalArgumentException When passed an illegal or inappropriate argument.
     */
    // TODO Consider refactoring to use AbstractFilter.AbstractFilterBuilder
    @PluginFactory
    public static RegexFilter createFilter(
            // @formatter:off
            @PluginAttribute("regex") final String regex,
            @PluginElement("PatternFlags") final String[] patternFlags,
            @PluginAttribute("useRawMsg") final Boolean useRawMsg,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch)
            // @formatter:on
            throws IllegalArgumentException, IllegalAccessException {
        if (regex == null) {
            LOGGER.error("A regular expression must be provided for RegexFilter");
            return null;
        }
        return new RegexFilter(
                Boolean.TRUE.equals(useRawMsg), Pattern.compile(regex, toPatternFlags(patternFlags)), match, mismatch);
    }

    private static int toPatternFlags(final String[] patternFlags)
            throws IllegalArgumentException, IllegalAccessException {
        if (patternFlags == null || patternFlags.length == 0) {
            return DEFAULT_PATTERN_FLAGS;
        }
        final Field[] fields = Pattern.class.getDeclaredFields();
        final Comparator<Field> comparator = (f1, f2) -> f1.getName().compareTo(f2.getName());
        Arrays.sort(fields, comparator);
        final String[] fieldNames = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldNames[i] = fields[i].getName();
        }
        int flags = DEFAULT_PATTERN_FLAGS;
        for (final String test : patternFlags) {
            final int index = Arrays.binarySearch(fieldNames, test);
            if (index >= 0) {
                final Field field = fields[index];
                flags |= field.getInt(Pattern.class);
            }
        }
        return flags;
    }
}
