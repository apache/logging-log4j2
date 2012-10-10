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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Formats a log event as a BSD Log record.
 */
@Plugin(name = "SyslogLayout", type = "Core", elementType = "layout", printObject = true)
public class SyslogLayout extends AbstractStringLayout {

    private final Facility facility;
    private final boolean includeNewLine;

    /**
     * Date format used if header = true.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss ", Locale.ENGLISH);
    /**
     * Host name used to identify messages from this appender.
     */
    private final String localHostname = getLocalHostname();


    protected SyslogLayout(Facility facility, boolean includeNL, Charset c) {
        super(c);
        this.facility = facility;
        this.includeNewLine = includeNL;
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent} in conformance with the log4j.dtd.
     *
     * @param event The LogEvent
     * @return the event formatted as a String.
     */
    public String toSerializable(final LogEvent event) {
        StringBuilder buf = new StringBuilder();

        buf.append("<");
        buf.append(Priority.getPriority(facility, event.getLevel()));
        buf.append(">");
        addDate(event.getMillis(), buf);
        buf.append(" ");
        buf.append(localHostname);
        buf.append(" ");
        buf.append(event.getMessage().getFormattedMessage());
        if (includeNewLine) {
            buf.append("\n");
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
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException uhe) {
            LOGGER.error("Could not determine local host name", uhe);
            return "UNKNOWN_LOCALHOST";
        }
    }

    private synchronized void addDate(final long timestamp, StringBuilder buf) {
        int index = buf.length() + 4;
        buf.append(dateFormat.format(new Date(timestamp)));
        //  RFC 3164 says leading space, not leading zero on days 1-9
        if (buf.charAt(index) == '0') {
            buf.setCharAt(index, ' ');
        }
    }

    /**
     * Create a SyslogLayout.
     * @param facility The Facility is used to try to classify the message.
     * @param includeNL If true a newline will be appended to the result.
     * @param charset The character set.
     * @return A SyslogLayout.
     */
    @PluginFactory
    public static SyslogLayout createLayout(@PluginAttr("facility") String facility,
                                            @PluginAttr("newLine") String includeNL,
                                            @PluginAttr("charset") String charset) {

        Charset c = Charset.isSupported("UTF-8") ? Charset.forName("UTF-8") : Charset.defaultCharset();
        if (charset != null) {
            if (Charset.isSupported(charset)) {
                c = Charset.forName(charset);
            } else {
                LOGGER.error("Charset " + charset + " is not supported for layout, using " + c.displayName());
            }
        }
        boolean includeNewLine = includeNL == null ? false : Boolean.valueOf(includeNL);
        Facility f = facility != null ? Facility.valueOf(facility.toUpperCase()) : Facility.LOCAL0;
        return new SyslogLayout(f, includeNewLine, c);
    }
}
