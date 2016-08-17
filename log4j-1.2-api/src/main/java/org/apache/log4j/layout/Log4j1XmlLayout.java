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
package org.apache.log4j.layout;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.util.Transform;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Port of XMLLayout in Log4j 1.x. Provided for compatibility with existing Log4j 1 configurations.
 *
 * Originally developed by Ceki G&uuml;lc&uuml;, Mathias Bogaert.
 */
@Plugin(name = "Log4j1XmlLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class Log4j1XmlLayout extends AbstractStringLayout {

    private final boolean locationInfo;
    private final boolean properties;

    @PluginFactory
    public static Log4j1XmlLayout createLayout(
            // @formatter:off
            @PluginAttribute(value = "locationInfo", defaultBoolean = false) final boolean locationInfo,
            @PluginAttribute(value = "properties", defaultBoolean = false) final boolean properties
            // @formatter:on
    ) {
        return new Log4j1XmlLayout(locationInfo, properties);
    }

    private Log4j1XmlLayout(boolean locationInfo, boolean properties) {
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

    private void formatTo(final LogEvent event, final StringBuilder buf) {
        // We yield to the \r\n heresy.

        buf.append("<log4j:event logger=\"");
        buf.append(Transform.escapeHtmlTags(event.getLoggerName()));
        buf.append("\" timestamp=\"");
        buf.append(event.getTimeMillis());
        buf.append("\" level=\"");
        buf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
        buf.append("\" thread=\"");
        buf.append(Transform.escapeHtmlTags(event.getThreadName()));
        buf.append("\">\r\n");

        buf.append("<log4j:message><![CDATA[");
        // Append the rendered message. Also make sure to escape any existing CDATA sections.
        Transform.appendEscapingCData(buf, event.getMessage().getFormattedMessage());
        buf.append("]]></log4j:message>\r\n");

        List<String> ndc = event.getContextStack().asList();
        if (!ndc.isEmpty()) {
            buf.append("<log4j:NDC><![CDATA[");
            Transform.appendEscapingCData(buf, StringUtils.join(ndc, ' '));
            buf.append("]]></log4j:NDC>\r\n");
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        Throwable thrown = event.getThrown();
        if (thrown != null) {
            buf.append("<log4j:throwable><![CDATA[");
            buf.append(thrown.toString());
            buf.append("\r\n");
            for (StackTraceElement element : thrown.getStackTrace()) {
                Transform.appendEscapingCData(buf, "\tat " + element.toString());
                buf.append("\r\n");
            }
            buf.append("]]></log4j:throwable>\r\n");
        }

        if (locationInfo) {
            StackTraceElement source = event.getSource();
            if (source != null) {
                buf.append("<log4j:locationInfo class=\"");
                buf.append(Transform.escapeHtmlTags(source.getClassName()));
                buf.append("\" method=\"");
                buf.append(Transform.escapeHtmlTags(source.getMethodName()));
                buf.append("\" file=\"");
                buf.append(Transform.escapeHtmlTags(source.getFileName()));
                buf.append("\" line=\"");
                buf.append(source.getLineNumber());
                buf.append("\"/>\r\n");
            }
        }

        if (properties) {
            Map<String, String> contextMap = event.getContextMap();
            if (!contextMap.isEmpty()) {
                buf.append("<log4j:properties>\r\n");
                Object[] keys = contextMap.keySet().toArray();
                Arrays.sort(keys);
                for (Object key1 : keys) {
                    String key = key1.toString();
                    String val = contextMap.get(key);
                    if (val != null) {
                        buf.append("<log4j:data name=\"");
                        buf.append(Transform.escapeHtmlTags(key));
                        buf.append("\" value=\"");
                        buf.append(Transform.escapeHtmlTags(val));
                        buf.append("\"/>\r\n");
                    }
                }
                buf.append("</log4j:properties>\r\n");
            }
        }

        buf.append("</log4j:event>\r\n\r\n");
    }

}
