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
package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.pattern.RegexReplacement;

import java.nio.charset.Charset;
import java.util.regex.Matcher;

/**
 * https://issues.apache.org/jira/browse/LOG4J2-969
 *
 * Log4j2 plugin for RFC3164 otherwise known as the BSD syslog format.
 * Composite Syslog Layout
 */
public class Rfc3164SyslogLayout extends SyslogLayout {

    private final PatternLayout patternLayout;

    protected Rfc3164SyslogLayout(final PatternLayout patternLayout, final Facility facility, final boolean includeNL, final String escapeNL, final Charset charset) {
        super(facility, includeNL, escapeNL, charset);

        this.patternLayout = patternLayout;
    }

    /**
     * format message content
     * @param event
     * @return the formatted content
     */
    protected String getLogContent(final LogEvent event){

        return patternLayout.toSerializable(event);
    }

    /**
     * Create a SyslogLayout.
     * @param facility The Facility is used to try to classify the message.
     * @param includeNewLine If true a newline will be appended to the result.
     * @param escapeNL Pattern to use for replacing newlines.
     * @param charset The character set.
     * @param pattern The pattern to use for the message part of the syslog
     * @param patternSelector Allows different patterns to be used based on some selection criteria.
     * @param config The Configuration. Some Converters require access to the Interpolator.
     * @param replace A Regex replacement String.
     * @param alwaysWriteExceptions
     *        If {@code "true"} (default) exceptions are always written even if the pattern contains no exception tokens.
     * @return A Rfc3164SyslogLayout.
     */
    @PluginFactory
    public static Rfc3164SyslogLayout createLayout(
            // syslog layout configuration
            @PluginAttribute(value = "facility", defaultString = "LOCAL0") final Facility facility,
            @PluginAttribute(value = "newLine", defaultBoolean = false) final boolean includeNewLine,
            @PluginAttribute("newLineEscape") final String escapeNL,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset,
            // pattern layout configuration
            @PluginAttribute(value = "pattern", defaultString = PatternLayout.DEFAULT_CONVERSION_PATTERN) String pattern,
            @PluginElement("PatternSelector") final PatternSelector patternSelector,
            @PluginConfiguration final Configuration config,
            @PluginElement("Replace") final RegexReplacement replace,
            @PluginAttribute(value = "alwaysWriteExceptions", defaultBoolean = true) final boolean alwaysWriteExceptions ) {

        final PatternLayout patternLayout =
                PatternLayout.createLayout(pattern, patternSelector, config, replace, charset, alwaysWriteExceptions,
                        false, null, null);

        return new Rfc3164SyslogLayout(patternLayout, facility, includeNewLine, escapeNL, charset);
    }

}
