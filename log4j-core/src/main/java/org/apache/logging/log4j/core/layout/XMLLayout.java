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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Charsets;
import org.apache.logging.log4j.core.helpers.Strings;
import org.apache.logging.log4j.core.helpers.Throwables;
import org.apache.logging.log4j.core.helpers.Transform;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;


/**
 * Appends a series of {@code event} elements as defined in the <a href="log4j.dtd">log4j.dtd</a>.
 *
 * <h4>Complete well-formed XML vs. fragment XML</h4>
 * <p>
 * If you configure {@code complete="true"}, the appender outputs a well-formed XML document where the default namespace
 * is the log4j namespace {@value #XML_NAMESPACE}. By default, with {@code complete="false"}, you should include the
 * output as an <em>external entity</em> in a separate file to form a well-formed XML document, in which case the
 * appender uses {@code namespacePrefix} with a default of {@value #DEFAULT_NS_PREFIX}.
 * </p>
 * <p>
 * A well-formed XML document follows this pattern:
 * </p>
 *
 * <pre>
 * &lt;?xml version="1.0" encoding=&quotUTF-8&quot?&gt;
 * &lt;Events xmlns="http://logging.apache.org/log4j/2.0/events"&gt;
 * &nbsp;&nbsp;&lt;Event logger="com.foo.Bar" timestamp="1373436580419" level="INFO" thread="main"&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;Message>&lt;![CDATA[This is a log message 1]]&gt;&lt;/Message&gt;
 * &nbsp;&nbsp;&lt;/Event&gt;
 * &nbsp;&nbsp;&lt;Event logger="com.foo.Baz" timestamp="1373436580420" level="INFO" thread="main"&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;Message>&lt;![CDATA[This is a log message 2]]&gt;&lt;/Message&gt;
 * &nbsp;&nbsp;&lt;/Event&gt;
 * &lt;/Events&gt;
 * </pre>
 * <p>
 * If {@code complete="false"}, the appender does not write the XML processing instruction and the root element.
 * </p>
 * <p>
 * This approach enforces the independence of the XMLLayout and the appender where you embed it.
 * </p>
 * <h4>Encoding</h4>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise
 * events containing non ASCII characters could result in corrupted log files.
 * </p>
 * <h4>Pretty vs. compact XML</h4>
 * <p>
 * By default, the XML layout is not compact (a.k.a. not "pretty") with {@code compact="false"}, which means the
 * appender uses end-of-line characters and indents lines to format the XML. If {@code compact="true"}, then no
 * end-of-line or indentation is used. Message content may contain, of course, end-of-lines.
 * </p>
 */
@Plugin(name = "XMLLayout", category = "Core", elementType = "layout", printObject = true)
public class XMLLayout extends AbstractStringLayout {

    private static final String XML_NAMESPACE = "http://logging.apache.org/log4j/2.0/events";
    private static final String ROOT_TAG = "Events";
    private static final int DEFAULT_SIZE = 256;

    // We yield to \r\n for the default.
    private static final String DEFAULT_EOL = "\r\n";
    private static final String COMPACT_EOL = "";
    private static final String DEFAULT_INDENT = "  ";
    private static final String COMPACT_INDENT = "";
    private static final String DEFAULT_NS_PREFIX = "log4j";

    private static final String[] FORMATS = new String[] {"xml"};

    private final boolean locationInfo;
    private final boolean properties;
    private final boolean complete;
    private final String namespacePrefix;
    private final String eol;
    private final String indent1;
    private final String indent2;
    private final String indent3;

    protected XMLLayout(final boolean locationInfo, final boolean properties, final boolean complete,
                        boolean compact, final String nsPrefix, final Charset charset) {
        super(charset);
        this.locationInfo = locationInfo;
        this.properties = properties;
        this.complete = complete;
        this.eol = compact ? COMPACT_EOL : DEFAULT_EOL;
        this.indent1 = compact ? COMPACT_INDENT : DEFAULT_INDENT;
        this.indent2 = this.indent1 + this.indent1;
        this.indent3 = this.indent2 + this.indent1;
        this.namespacePrefix = (Strings.isEmpty(nsPrefix) ? DEFAULT_NS_PREFIX : nsPrefix) + ":";
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent} in conformance with the log4j.dtd.
     *
     * @param event The LogEvent.
     * @return The XML representation of the LogEvent.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder buf = new StringBuilder(DEFAULT_SIZE);

        buf.append(this.indent1);
        buf.append('<');
        if (!complete) {
            buf.append(this.namespacePrefix);
        }
        buf.append("Event logger=\"");
        String name = event.getLoggerName();
        if (name.isEmpty()) {
            name = "root";
        }
        buf.append(Transform.escapeHtmlTags(name));
        buf.append("\" timestamp=\"");
        buf.append(event.getMillis());
        buf.append("\" level=\"");
        buf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
        buf.append("\" thread=\"");
        buf.append(Transform.escapeHtmlTags(event.getThreadName()));
        buf.append("\">");
        buf.append(this.eol);

        final Message msg = event.getMessage();
        if (msg != null) {
            boolean xmlSupported = false;
            if (msg instanceof MultiformatMessage) {
                final String[] formats = ((MultiformatMessage) msg).getFormats();
                for (final String format : formats) {
                    if (format.equalsIgnoreCase("XML")) {
                        xmlSupported = true;
                        break;
                    }
                }
            }
            buf.append(this.indent2);
            buf.append('<');
            if (!complete) {
                buf.append(this.namespacePrefix);
            }
            buf.append("Message>");
            if (xmlSupported) {
                buf.append(((MultiformatMessage) msg).getFormattedMessage(FORMATS));
            } else {
                buf.append("<![CDATA[");
                // Append the rendered message. Also make sure to escape any
                // existing CDATA sections.
                Transform.appendEscapingCDATA(buf, event.getMessage().getFormattedMessage());
                buf.append("]]>");
            }
            buf.append("</");
            if (!complete) {
                buf.append(this.namespacePrefix);
            }
            buf.append("Message>");
            buf.append(this.eol);
        }

        if (event.getContextStack().getDepth() > 0) {
            buf.append(this.indent2);
            buf.append('<');
            if (!complete) {
                buf.append(this.namespacePrefix);
            }
            buf.append("NDC><![CDATA[");
            Transform.appendEscapingCDATA(buf, event.getContextStack().toString());
            buf.append("]]></");
            if (!complete) {
                buf.append(this.namespacePrefix);
            }
            buf.append("NDC>");
            buf.append(this.eol);
        }

        final Throwable throwable = event.getThrown();
        if (throwable != null) {
            final List<String> s = Throwables.toStringList(throwable);
            buf.append(this.indent2);
            buf.append('<');
            if (!complete) {
                buf.append(this.namespacePrefix);
            }
            buf.append("Throwable><![CDATA[");
            for (final String str : s) {
                Transform.appendEscapingCDATA(buf, str);
                buf.append(this.eol);
            }
            buf.append("]]></");
            if (!complete) {
                buf.append(this.namespacePrefix);
            }
            buf.append("Throwable>");
            buf.append(this.eol);
        }

        if (locationInfo) {
            final StackTraceElement element = event.getSource();
            buf.append(this.indent2);
            buf.append('<');
            if (!complete) {
                buf.append(this.namespacePrefix);
            }
            buf.append("LocationInfo class=\"");
            buf.append(Transform.escapeHtmlTags(element.getClassName()));
            buf.append("\" method=\"");
            buf.append(Transform.escapeHtmlTags(element.getMethodName()));
            buf.append("\" file=\"");
            buf.append(Transform.escapeHtmlTags(element.getFileName()));
            buf.append("\" line=\"");
            buf.append(element.getLineNumber());
            buf.append("\"/>");
            buf.append(this.eol);
        }

        if (properties && event.getContextMap().size() > 0) {
            buf.append(this.indent2);
            buf.append('<');
            if (!complete) {
                buf.append(this.namespacePrefix);
            }
            buf.append("Properties>");
            buf.append(this.eol);
            for (final Map.Entry<String, String> entry : event.getContextMap().entrySet()) {
                buf.append(this.indent3);
                buf.append('<');
                if (!complete) {
                    buf.append(this.namespacePrefix);
                }
                buf.append("Data name=\"");
                buf.append(Transform.escapeHtmlTags(entry.getKey()));
                buf.append("\" value=\"");
                buf.append(Transform.escapeHtmlTags(String.valueOf(entry.getValue())));
                buf.append("\"/>");
                buf.append(this.eol);
            }
            buf.append(this.indent2);
            buf.append("</");
            if (!complete) {
                buf.append(this.namespacePrefix);
            }
            buf.append("Properties>");
            buf.append(this.eol);
        }

        buf.append(this.indent1);
        buf.append("</");
        if (!complete) {
            buf.append(this.namespacePrefix);
        }
        buf.append("Event>");
        buf.append(this.eol);

        return buf.toString();
    }

    /**
     * Returns appropriate XML headers.
     * <ol>
     * <li>XML processing instruction</li>
     * <li>XML root element</li>
     * </ol>
     *
     * @return a byte array containing the header.
     */
    @Override
    public byte[] getHeader() {
        if (!complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\" encoding=\"");
        buf.append(this.getCharset().name());
        buf.append("\"?>");
        buf.append(this.eol);
        // Make the log4j namespace the default namespace, no need to use more space with a namespace prefix.
        buf.append('<');
        buf.append(ROOT_TAG);
        buf.append(" xmlns=\"" + XML_NAMESPACE + "\">");
        buf.append(this.eol);
        return buf.toString().getBytes(this.getCharset());
    }


    /**
     * Returns appropriate XML footer.
     *
     * @return a byte array containing the footer, closing the XML root element.
     */
    @Override
    public byte[] getFooter() {
        if (!complete) {
            return null;
        }
        return ("</" + ROOT_TAG + ">" + this.eol).getBytes(getCharset());
    }

    /**
     * XMLLayout's content format is specified by:<p/>
     * Key: "dtd" Value: "log4j-events.dtd"<p/>
     * Key: "version" Value: "2.0"
     * @return Map of content format keys supporting XMLLayout
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<String, String>();
        //result.put("dtd", "log4j-events.dtd");
        result.put("xsd", "log4j-events.xsd");
        result.put("version", "2.0");
        return result;
    }

    @Override
    /**
     * @return The content type.
     */
    public String getContentType() {
        return "text/xml; charset=" + this.getCharset();
    }

    /**
     * Creates an XML Layout.
     *
     * @param locationInfo If "true", includes the location information in the generated XML.
     * @param properties If "true", includes the thread context in the generated XML.
     * @param completeStr If "true", includes the XML header and footer, defaults to "false".
     * @param compactStr If "true", does not use end-of-lines and indentation, defaults to "false".
     * @param namespacePrefix The namespace prefix, defaults to {@value #DEFAULT_NS_PREFIX}
     * @param charsetName The character set to use, if {@code null}, uses "UTF-8".
     * @return An XML Layout.
     */
    @PluginFactory
    public static XMLLayout createLayout(
            @PluginAttribute("locationInfo") final String locationInfo,
            @PluginAttribute("properties") final String properties,
            @PluginAttribute("complete") final String completeStr,
            @PluginAttribute("compact") final String compactStr,
            @PluginAttribute("namespacePrefix") final String namespacePrefix,
            @PluginAttribute("charset") final String charsetName) {
        final Charset charset = Charsets.getSupportedCharset(charsetName, Charsets.UTF_8);
        final boolean info = Boolean.parseBoolean(locationInfo);
        final boolean props = Boolean.parseBoolean(properties);
        final boolean complete = Boolean.parseBoolean(completeStr);
        final boolean compact = Boolean.parseBoolean(compactStr);
        return new XMLLayout(info, props, complete, compact, namespacePrefix, charset);
    }
}
