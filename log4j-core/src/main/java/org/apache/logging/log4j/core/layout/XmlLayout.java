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
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.jackson.XmlConstants;
import org.apache.logging.log4j.core.util.KeyValuePair;

/**
 * Appends a series of {@code event} elements as defined in the <a href="log4j.dtd">log4j.dtd</a>.
 *
 * <h2>Complete well-formed XML vs. fragment XML</h2>
 * <p>
 * If you configure {@code complete="true"}, the appender outputs a well-formed XML document where the default namespace
 * is the log4j namespace {@value XmlConstants#XML_NAMESPACE}. By default, with {@code complete="false"}, you should
 * include the output as an <em>external entity</em> in a separate file to form a well-formed XML document.
 * </p>
 * <p>
 * If {@code complete="false"}, the appender does not write the XML processing instruction and the root element.
 * </p>
 * <h2>Encoding</h2>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise
 * events containing non-ASCII characters could result in corrupted log files.
 * </p>
 * <h2>Pretty vs. compact XML</h2>
 * <p>
 * By default, the XML layout is not compact (compact = not "pretty") with {@code compact="false"}, which means the
 * appender uses end-of-line characters and indents lines to format the XML. If {@code compact="true"}, then no
 * end-of-line or indentation is used. Message content may contain, of course, end-of-lines.
 * </p>
 * <h2>Additional Fields</h2>
 * <p>
 * This property allows addition of custom fields into generated JSON.
 * {@code <XmlLayout><KeyValuePair key="foo" value="bar"/></XmlLayout>} inserts {@code <foo>bar</foo>} directly
 * into XML output. Supports Lookup expressions.
 * </p>
 */
@Plugin(name = "XmlLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class XmlLayout extends AbstractJacksonLayout {

    private static final String ROOT_TAG = "Events";

    public static class Builder<B extends Builder<B>> extends AbstractJacksonLayout.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<XmlLayout> {

        public Builder() {
            setCharset(StandardCharsets.UTF_8);
        }

        @Override
        public XmlLayout build() {
            return new XmlLayout(
                    getConfiguration(),
                    isLocationInfo(),
                    isProperties(),
                    isComplete(),
                    isCompact(),
                    getEndOfLine(),
                    getCharset(),
                    isIncludeStacktrace(),
                    isStacktraceAsString(),
                    isIncludeNullDelimiter(),
                    isIncludeTimeMillis(),
                    getAdditionalFields());
        }
    }

    /**
     * @deprecated Use {@link #newBuilder()} instead
     */
    @Deprecated
    protected XmlLayout(
            final boolean locationInfo,
            final boolean properties,
            final boolean complete,
            final boolean compact,
            final Charset charset,
            final boolean includeStacktrace) {
        this(
                null,
                locationInfo,
                properties,
                complete,
                compact,
                null,
                charset,
                includeStacktrace,
                false,
                false,
                false,
                null);
    }

    private XmlLayout(
            final Configuration config,
            final boolean locationInfo,
            final boolean properties,
            final boolean complete,
            final boolean compact,
            final String endOfLine,
            final Charset charset,
            final boolean includeStacktrace,
            final boolean stacktraceAsString,
            final boolean includeNullDelimiter,
            final boolean includeTimeMillis,
            final KeyValuePair[] additionalFields) {
        super(
                config,
                new JacksonFactory.XML(includeStacktrace, stacktraceAsString)
                        .newWriter(locationInfo, properties, compact, includeTimeMillis),
                charset,
                compact,
                complete,
                false,
                endOfLine,
                null,
                null,
                includeNullDelimiter,
                additionalFields);
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
        buf.append(" xmlns=\"" + XmlConstants.XML_NAMESPACE + "\">");
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
        return getBytes("</" + ROOT_TAG + '>' + this.eol);
    }

    /**
     * Gets this XmlLayout's content format. Specified by:
     * <ul>
     * <li>Key: "dtd" Value: "log4j-events.dtd"</li>
     * <li>Key: "version" Value: "2.0"</li>
     * </ul>
     *
     * @return Map of content format keys supporting XmlLayout
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        // result.put("dtd", "log4j-events.dtd");
        result.put("xsd", "log4j-events.xsd");
        result.put("version", "2.0");
        return result;
    }

    /**
     * @return The content type.
     */
    @Override
    public String getContentType() {
        return "text/xml; charset=" + this.getCharset();
    }

    /**
     * Creates an XML Layout.
     *
     * @param locationInfo If "true", includes the location information in the generated XML.
     * @param properties If "true", includes the thread context map in the generated XML.
     * @param complete If "true", includes the XML header and footer, defaults to "false".
     * @param compact If "true", does not use end-of-lines and indentation, defaults to "false".
     * @param charset The character set to use, if {@code null}, uses "UTF-8".
     * @param includeStacktrace
     *            If "true", includes the stacktrace of any Throwable in the generated XML, defaults to "true".
     * @return An XML Layout.
     *
     * @deprecated Use {@link #newBuilder()} instead
     */
    @Deprecated
    public static XmlLayout createLayout(
            final boolean locationInfo,
            final boolean properties,
            final boolean complete,
            final boolean compact,
            final Charset charset,
            final boolean includeStacktrace) {
        return new XmlLayout(
                null,
                locationInfo,
                properties,
                complete,
                compact,
                null,
                charset,
                includeStacktrace,
                false,
                false,
                false,
                null);
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Creates an XML Layout using the default settings.
     *
     * @return an XML Layout.
     */
    public static XmlLayout createDefaultLayout() {
        return new XmlLayout(
                null, false, false, false, false, null, StandardCharsets.UTF_8, true, false, false, false, null);
    }
}
