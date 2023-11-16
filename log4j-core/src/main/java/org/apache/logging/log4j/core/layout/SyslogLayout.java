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
package org.apache.logging.log4j.core.layout;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.util.Chars;

/**
 * Formats a log event as a BSD Log record.
 */
@Plugin(name = "SyslogLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class SyslogLayout extends AbstractStringLayout {

    /**
     * Builds a SyslogLayout.
     * <p>The main arguments are</p>
     * <ul>
     * <li>facility: The Facility is used to try to classify the message.</li>
     * <li>includeNewLine: If true a newline will be appended to the result.</li>
     * <li>escapeNL: Pattern to use for replacing newlines.</li>
     * <li>charset: The character set.</li>
     * </ul>
     * @param <B> the builder type
     */
    public static class Builder<B extends Builder<B>> extends AbstractStringLayout.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<SyslogLayout> {

        public Builder() {
            setCharset(StandardCharsets.UTF_8);
        }

        @PluginBuilderAttribute
        private Facility facility = Facility.LOCAL0;

        @PluginBuilderAttribute("newLine")
        private boolean includeNewLine;

        @PluginBuilderAttribute("newLineEscape")
        private String escapeNL;

        @Override
        public SyslogLayout build() {
            return new SyslogLayout(facility, includeNewLine, escapeNL, getCharset());
        }

        public Facility getFacility() {
            return facility;
        }

        public boolean isIncludeNewLine() {
            return includeNewLine;
        }

        public String getEscapeNL() {
            return escapeNL;
        }

        public B setFacility(final Facility facility) {
            this.facility = facility;
            return asBuilder();
        }

        public B setIncludeNewLine(final boolean includeNewLine) {
            this.includeNewLine = includeNewLine;
            return asBuilder();
        }

        public B setEscapeNL(final String escapeNL) {
            this.escapeNL = escapeNL;
            return asBuilder();
        }
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Match newlines in a platform-independent manner.
     */
    public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");

    private final Facility facility;
    private final boolean includeNewLine;
    private final String escapeNewLine;

    /**
     * Date format used if header = true.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss", Locale.ENGLISH);

    /**
     * Host name used to identify messages from this appender.
     */
    private final String localHostname = NetUtils.getLocalHostname();

    protected SyslogLayout(
            final Facility facility, final boolean includeNL, final String escapeNL, final Charset charset) {
        super(charset);
        this.facility = facility;
        this.includeNewLine = includeNL;
        this.escapeNewLine = escapeNL == null ? null : Matcher.quoteReplacement(escapeNL);
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent} in conformance with the BSD Log record format.
     *
     * @param event The LogEvent
     * @return the event formatted as a String.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder buf = getStringBuilder();

        buf.append('<');
        buf.append(Priority.getPriority(facility, event.getLevel()));
        buf.append('>');
        addDate(event.getTimeMillis(), buf);
        buf.append(Chars.SPACE);
        buf.append(localHostname);
        buf.append(Chars.SPACE);

        String message = event.getMessage().getFormattedMessage();
        if (null != escapeNewLine) {
            message = NEWLINE_PATTERN.matcher(message).replaceAll(escapeNewLine);
        }
        buf.append(message);

        if (includeNewLine) {
            buf.append(Chars.LF);
        }
        return buf.toString();
    }

    private synchronized void addDate(final long timestamp, final StringBuilder buf) {
        final int index = buf.length() + 4;
        buf.append(dateFormat.format(new Date(timestamp)));
        //  RFC 3164 says leading space, not leading zero on days 1-9
        if (buf.charAt(index) == '0') {
            buf.setCharAt(index, Chars.SPACE);
        }
    }

    /**
     * Gets this SyslogLayout's content format. Specified by:
     * <ul>
     * <li>Key: "structured" Value: "false"</li>
     * <li>Key: "dateFormat" Value: "MMM dd HH:mm:ss"</li>
     * <li>Key: "format" Value: "&lt;LEVEL&gt;TIMESTAMP PROP(HOSTNAME) MESSAGE"</li>
     * <li>Key: "formatType" Value: "logfilepatternreceiver" (format uses the keywords supported by
     * LogFilePatternReceiver)</li>
     * </ul>
     *
     * @return Map of content format keys supporting SyslogLayout
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        result.put("structured", "false");
        result.put("formatType", "logfilepatternreceiver");
        result.put("dateFormat", dateFormat.toPattern());
        result.put("format", "<LEVEL>TIMESTAMP PROP(HOSTNAME) MESSAGE");
        return result;
    }

    /**
     * Creates a SyslogLayout.
     *
     * @param facility The Facility is used to try to classify the message.
     * @param includeNewLine If true a newline will be appended to the result.
     * @param escapeNL Pattern to use for replacing newlines.
     * @param charset The character set.
     * @return A SyslogLayout.
     * @deprecated Use {@link #newBuilder()}.
     */
    @Deprecated
    public static SyslogLayout createLayout(
            final Facility facility, final boolean includeNewLine, final String escapeNL, final Charset charset) {
        return new SyslogLayout(facility, includeNewLine, escapeNL, charset);
    }

    /**
     * Gets the facility.
     *
     * @return the facility
     */
    public Facility getFacility() {
        return facility;
    }
}
