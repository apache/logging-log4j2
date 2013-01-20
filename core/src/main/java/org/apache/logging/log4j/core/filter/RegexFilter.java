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
package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This filter returns the onMatch result if the message matches the regular expression.
 *
 * The "useRawMsg" attribute can be used to indicate whether the regular expression should be
 * applied to the result of calling Message.getMessageFormat (true) or Message.getFormattedMessage()
 * (false). The default is false.
 *
 */
@Plugin(name = "RegexFilter", type = "Core", elementType = "filter", printObject = true)
public final class RegexFilter extends AbstractFilter {

    private final Pattern pattern;
    private final boolean useRawMessage;

    private RegexFilter(final boolean raw, final Pattern pattern, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.pattern = pattern;
        this.useRawMessage = raw;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                         final Object... params) {
        return filter(msg);
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                         final Throwable t) {
        return filter(msg.toString());
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        final String text = useRawMessage ? msg.getFormat() : msg.getFormattedMessage();
        return filter(text);
    }

    @Override
    public Result filter(final LogEvent event) {
        final String text = useRawMessage ? event.getMessage().getFormat() : event.getMessage().getFormattedMessage();
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
        final StringBuilder sb = new StringBuilder();
        sb.append("useRaw=").append(useRawMessage);
        sb.append(", pattern=").append(pattern.toString());
        return sb.toString();
    }

    /**
     * Create a Filter that matches a regular expression.
     * @param regex The regular expression to match.
     * @param useRawMsg If true, the raw message will be used, otherwise the formatted message will be used.
     * @param match The action to perform when a match occurs.
     * @param mismatch The action to perform when a mismatch occurs.
     * @return The RegexFilter.
     */
    @PluginFactory
    public static RegexFilter createFilter(@PluginAttr("regex") final String regex,
                                           @PluginAttr("useRawMsg") final String useRawMsg,
                                            @PluginAttr("onMatch") final String match,
                                            @PluginAttr("onMismatch") final String mismatch) {

        if (regex == null) {
            LOGGER.error("A regular expression must be provided for RegexFilter");
            return null;
        }
        final boolean raw = useRawMsg == null ? false : Boolean.parseBoolean(useRawMsg);
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (final Exception ex) {
            LOGGER.error("RegexFilter caught exception compiling pattern: " + regex + " cause: " + ex.getMessage());
            return null;
        }
        final Result onMatch = Result.toResult(match);
        final Result onMismatch = Result.toResult(mismatch);

        return new RegexFilter(raw, pattern, onMatch, onMismatch);
    }

}
