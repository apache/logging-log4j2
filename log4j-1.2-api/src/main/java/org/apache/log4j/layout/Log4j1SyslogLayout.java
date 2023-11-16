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
package org.apache.log4j.layout;

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.pattern.DatePatternConverter;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.util.Chars;

/**
 * Port of the layout used by SyslogAppender in Log4j 1.x. Provided for
 * compatibility with existing Log4j 1 configurations.
 *
 * Originally developed by Ceki G&uuml;lc&uuml; and Anders Kristensen.
 */
@Plugin(name = "Log4j1SyslogLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class Log4j1SyslogLayout extends AbstractStringLayout {

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
            implements org.apache.logging.log4j.core.util.Builder<Log4j1SyslogLayout> {

        public Builder() {
            setCharset(StandardCharsets.UTF_8);
        }

        @PluginBuilderAttribute
        private Facility facility = Facility.USER;

        @PluginBuilderAttribute
        private boolean facilityPrinting;

        @PluginBuilderAttribute
        private boolean header;

        @PluginElement("Layout")
        private Layout<? extends Serializable> messageLayout;

        @Override
        public Log4j1SyslogLayout build() {
            if (!isValid()) {
                return null;
            }
            if (messageLayout != null && !(messageLayout instanceof StringLayout)) {
                LOGGER.error("Log4j1SyslogLayout: the message layout must be a StringLayout.");
                return null;
            }
            return new Log4j1SyslogLayout(
                    facility, facilityPrinting, header, (StringLayout) messageLayout, getCharset());
        }

        public Facility getFacility() {
            return facility;
        }

        public boolean isFacilityPrinting() {
            return facilityPrinting;
        }

        public boolean isHeader() {
            return header;
        }

        public Layout<? extends Serializable> getMessageLayout() {
            return messageLayout;
        }

        public B setFacility(final Facility facility) {
            this.facility = facility;
            return asBuilder();
        }

        public B setFacilityPrinting(final boolean facilityPrinting) {
            this.facilityPrinting = facilityPrinting;
            return asBuilder();
        }

        public B setHeader(final boolean header) {
            this.header = header;
            return asBuilder();
        }

        public B setMessageLayout(final Layout<? extends Serializable> messageLayout) {
            this.messageLayout = messageLayout;
            return asBuilder();
        }
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Host name used to identify messages from this appender.
     */
    private static final String localHostname = NetUtils.getLocalHostname();

    private final Facility facility;
    private final boolean facilityPrinting;
    private final boolean header;
    private final StringLayout messageLayout;

    /**
     * Date format used if header = true.
     */
    private static final String[] dateFormatOptions = {"MMM dd HH:mm:ss", null, "en"};

    private final LogEventPatternConverter dateConverter = DatePatternConverter.newInstance(dateFormatOptions);

    private Log4j1SyslogLayout(
            final Facility facility,
            final boolean facilityPrinting,
            final boolean header,
            final StringLayout messageLayout,
            final Charset charset) {
        super(charset);
        this.facility = facility;
        this.facilityPrinting = facilityPrinting;
        this.header = header;
        this.messageLayout = messageLayout;
    }

    /**
     * Formats a {@link LogEvent} in conformance with the BSD Log record format.
     *
     * @param event The LogEvent
     * @return the event formatted as a String.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        // The messageLayout also uses the thread-bound StringBuilder,
        // so we generate the message first
        final String message = messageLayout != null
                ? messageLayout.toSerializable(event)
                : event.getMessage().getFormattedMessage();
        final StringBuilder buf = getStringBuilder();

        buf.append('<');
        buf.append(Priority.getPriority(facility, event.getLevel()));
        buf.append('>');

        if (header) {
            final int index = buf.length() + 4;
            dateConverter.format(event, buf);
            // RFC 3164 says leading space, not leading zero on days 1-9
            if (buf.charAt(index) == '0') {
                buf.setCharAt(index, Chars.SPACE);
            }

            buf.append(Chars.SPACE);
            buf.append(localHostname);
            buf.append(Chars.SPACE);
        }

        if (facilityPrinting) {
            buf.append(facility != null ? toRootLowerCase(facility.name()) : "user")
                    .append(':');
        }

        buf.append(message);
        // TODO: splitting message into 1024 byte chunks?
        return buf.toString();
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
        result.put("dateFormat", dateFormatOptions[0]);
        if (header) {
            result.put("format", "<LEVEL>TIMESTAMP PROP(HOSTNAME) MESSAGE");
        } else {
            result.put("format", "<LEVEL>MESSAGE");
        }
        return result;
    }
}
