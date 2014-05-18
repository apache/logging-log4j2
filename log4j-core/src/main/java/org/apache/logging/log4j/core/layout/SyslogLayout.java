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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.util.Charsets;


/**
 * Formats a log event as a BSD Log record.
 */
@Plugin(name = "SyslogLayout", category = "Core", elementType = "layout", printObject = true)
public final class SyslogLayout extends AbstractStringLayout {
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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss ", Locale.ENGLISH);
    /**
     * Host name used to identify messages from this appender.
     */
    private final String localHostname = getLocalHostname();



    protected SyslogLayout(final Facility facility, final boolean includeNL, final String escapeNL, final Charset charset) {
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
        final StringBuilder buf = new StringBuilder();

        buf.append('<');
        buf.append(Priority.getPriority(facility, event.getLevel()));
        buf.append('>');
        addDate(event.getTimeMillis(), buf);
        buf.append(' ');
        buf.append(localHostname);
        buf.append(' ');

        String message = event.getMessage().getFormattedMessage();
        if (null != escapeNewLine) {
            message = NEWLINE_PATTERN.matcher(message).replaceAll(escapeNewLine);
        }
        buf.append(message);

        if (includeNewLine) {
            buf.append('\n');
        }
        return buf.toString();
    }

    /**
     * This method gets the network name of the machine we are running on.
     * Returns "UNKNOWN_LOCALHOST" in the unlikely case where the host name
     * cannot be found.
     *
     * @return String the name of the local host
     */
    private String getLocalHostname() {
        try {
            final InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (final UnknownHostException uhe) {
            LOGGER.error("Could not determine local host name", uhe);
            return "UNKNOWN_LOCALHOST";
        }
    }

    private synchronized void addDate(final long timestamp, final StringBuilder buf) {
        final int index = buf.length() + 4;
        buf.append(dateFormat.format(new Date(timestamp)));
        //  RFC 3164 says leading space, not leading zero on days 1-9
        if (buf.charAt(index) == '0') {
            buf.setCharAt(index, ' ');
        }
    }

    /**
     * SyslogLayout's content format is specified by:<p/>
     * Key: "structured" Value: "false"<p/>
     * Key: "dateFormat" Value: "MMM dd HH:mm:ss "<p/>
     * Key: "format" Value: "<LEVEL>TIMESTAMP PROP(HOSTNAME) MESSAGE"<p/>
     * Key: "formatType" Value: "logfilepatternreceiver" (format uses the keywords supported by LogFilePatternReceiver)
     * @return Map of content format keys supporting SyslogLayout
     */
    @Override
    public Map<String, String> getContentFormat()
    {
        final Map<String, String> result = new HashMap<String, String>();
        result.put("structured", "false");
        result.put("formatType", "logfilepatternreceiver");
        result.put("dateFormat", dateFormat.toPattern());
        result.put("format", "<LEVEL>TIMESTAMP PROP(HOSTNAME) MESSAGE");
        return result;
    }

    /**
     * Create a SyslogLayout.
     * @param facility The Facility is used to try to classify the message.
     * @param includeNL If true a newline will be appended to the result.
     * @param escapeNL Pattern to use for replacing newlines.
     * @param charsetName The character set.
     * @return A SyslogLayout.
     */
    @PluginFactory
    public static SyslogLayout createLayout(
            @PluginAttribute("facility") final String facility,
            @PluginAttribute("newLine") final String includeNL,
            @PluginAttribute("newLineEscape") final String escapeNL,
            @PluginAttribute("charset") final String charsetName) {
        final Charset charset = Charsets.getSupportedCharset(charsetName);
        final boolean includeNewLine = Boolean.parseBoolean(includeNL);
        final Facility f = Facility.toFacility(facility, Facility.LOCAL0);
        return new SyslogLayout(f, includeNewLine, escapeNL, charset);
    }
}
