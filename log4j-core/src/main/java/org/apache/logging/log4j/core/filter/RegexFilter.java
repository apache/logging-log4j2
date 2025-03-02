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
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

/**
 * This filter returns the onMatch result if the message matches the regular expression.
 *
 * The "useRawMsg" attribute can be used to indicate whether the regular expression should be applied to the result of
 * calling Message.getMessageFormat (true) or Message.getFormattedMessage() (false). The default is false.
 *
 */
@Configurable(elementType = Filter.ELEMENT_TYPE, printObject = true)
@Plugin
public final class RegexFilter extends AbstractFilter {

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
        final String text = useRawMessage ? msg.getFormat() : msg.getFormattedMessage();
        return filter(text);
    }

    @Override
    public Result filter(final LogEvent event) {
        final String text = useRawMessage
                ? event.getMessage().getFormat()
                : event.getMessage().getFormattedMessage();
        return filter(text);
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
        return "useRaw=" + useRawMessage + ", pattern=" + pattern.toString();
    }

    /**
     * Creates a Filter that matches a regular expression.
     *
     * @param regex
     *        The regular expression to match.
     * @param patternFlags
     *        An array of Strings where each String is a {@link Pattern#compile(String, int)} compilation flag.
     *        (no longer used - pattern flags can be embedded in regex-expression.
     * @param useRawMsg
     *        If true, the raw message will be used, otherwise the formatted message will be used.
     * @param onMatch
     *        The action to perform when a match occurs.
     * @param onMismatch
     *        The action to perform when a mismatch occurs.
     * @return The RegexFilter.
     * @throws IllegalAccessException  When there is no access to the definition of the specified member.
     * @throws IllegalArgumentException When passed an illegal or inappropriate argument.
     * @deprecated use {@link #createFilter(String, Boolean, Result, Result)}
     */
    @Deprecated
    // TODO Consider refactoring to use AbstractFilter.AbstractFilterBuilder
    public static RegexFilter createFilter(
            // @formatter:off
            @PluginAttribute("regex") final String regex,
            final String[] patternFlags,
            @PluginAttribute("useRawMsg") final Boolean useRawMsg,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch)
            // @formatter:on
            throws IllegalArgumentException, IllegalAccessException {

        // LOG4J-3086 - pattern-flags can be embedded in RegEx expression

        return createFilter(regex, useRawMsg, match, mismatch);
    }

    /**
     * Creates a Filter that matches a regular expression.
     *
     * @param regex
     *        The regular expression to match.
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
            @PluginAttribute("useRawMsg") final Boolean useRawMsg,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch)
            // @formatter:on
            throws IllegalArgumentException, IllegalAccessException {
        boolean raw = Boolean.TRUE.equals(useRawMsg);
        if (regex == null) {
            LOGGER.error("A regular expression must be provided for RegexFilter");
            return null;
        }
        final Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (final Exception ex) {
            LOGGER.error("Unable to compile regular expression: {}", regex, ex);
            return null;
        }
        return new RegexFilter(raw, pattern, match, mismatch);
    }
}
