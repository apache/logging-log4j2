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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.util.Transform;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.Strings;

/**
 * Port of XMLLayout in Log4j 1.x. Provided for compatibility with existing Log4j 1 configurations.
 *
 * Originally developed by Ceki G&uuml;lc&uuml;, Mathias Bogaert.
 */
@Plugin(name = "Log4j1XmlLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class Log4j1XmlLayout extends AbstractStringLayout {

    /** We yield to the \r\n heresy. */
    private static final String EOL = "\r\n";

    private final boolean locationInfo;
    private final boolean properties;

    @PluginFactory
    public static Log4j1XmlLayout createLayout(
            // @formatter:off
            @PluginAttribute(value = "locationInfo") final boolean locationInfo,
            @PluginAttribute(value = "properties") final boolean properties
            // @formatter:on
            ) {
        return new Log4j1XmlLayout(locationInfo, properties);
    }

    private Log4j1XmlLayout(final boolean locationInfo, final boolean properties) {
        super(StandardCharsets.UTF_8);
        this.locationInfo = locationInfo;
        this.properties = properties;
    }

    public boolean isLocationInfo() {
        return locationInfo;
    }

    public boolean isProperties() {
        return properties;
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        final StringBuilder text = getStringBuilder();
        formatTo(event, text);
        getStringBuilderEncoder().encode(text, destination);
    }

    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder text = getStringBuilder();
        formatTo(event, text);
        return text.toString();
    }

    @SuppressFBWarnings(
            value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
            justification = "The throwable is formatted into a log file, which should be private.")
    private void formatTo(final LogEvent event, final StringBuilder buf) {
        buf.append("<log4j:event logger=\"");
        buf.append(Transform.escapeHtmlTags(event.getLoggerName()));
        buf.append("\" timestamp=\"");
        buf.append(event.getTimeMillis());
        buf.append("\" level=\"");
        buf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
        buf.append("\" thread=\"");
        buf.append(Transform.escapeHtmlTags(event.getThreadName()));
        buf.append("\">");
        buf.append(EOL);

        buf.append("<log4j:message><![CDATA[");
        // Append the rendered message. Also make sure to escape any existing CDATA sections.
        Transform.appendEscapingCData(buf, event.getMessage().getFormattedMessage());
        buf.append("]]></log4j:message>");
        buf.append(EOL);

        final List<String> ndc = event.getContextStack().asList();
        if (!ndc.isEmpty()) {
            buf.append("<log4j:NDC><![CDATA[");
            Transform.appendEscapingCData(buf, Strings.join(ndc, ' '));
            buf.append("]]></log4j:NDC>");
            buf.append(EOL);
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        final Throwable thrown = event.getThrown();
        if (thrown != null) {
            buf.append("<log4j:throwable><![CDATA[");
            final StringWriter w = new StringWriter();
            thrown.printStackTrace(new PrintWriter(w));
            Transform.appendEscapingCData(buf, w.toString());
            buf.append("]]></log4j:throwable>");
            buf.append(EOL);
        }

        if (locationInfo) {
            final StackTraceElement source = event.getSource();
            if (source != null) {
                buf.append("<log4j:locationInfo class=\"");
                buf.append(Transform.escapeHtmlTags(source.getClassName()));
                buf.append("\" method=\"");
                buf.append(Transform.escapeHtmlTags(source.getMethodName()));
                buf.append("\" file=\"");
                buf.append(Transform.escapeHtmlTags(source.getFileName()));
                buf.append("\" line=\"");
                buf.append(source.getLineNumber());
                buf.append("\"/>");
                buf.append(EOL);
            }
        }

        if (properties) {
            final ReadOnlyStringMap contextMap = event.getContextData();
            if (!contextMap.isEmpty()) {
                buf.append("<log4j:properties>\r\n");
                contextMap.forEach((key, val) -> {
                    if (val != null) {
                        buf.append("<log4j:data name=\"");
                        buf.append(Transform.escapeHtmlTags(key));
                        buf.append("\" value=\"");
                        buf.append(Transform.escapeHtmlTags(Objects.toString(val, null)));
                        buf.append("\"/>");
                        buf.append(EOL);
                    }
                });
                buf.append("</log4j:properties>");
                buf.append(EOL);
            }
        }

        buf.append("</log4j:event>");
        buf.append(EOL);
        buf.append(EOL);
    }
}
