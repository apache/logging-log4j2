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

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Appends a series of YAML events as strings serialized as bytes.
 *
 * <h3>Complete well-formed YAML vs. fragment YAML</h3>
 * <p>
 * If you configure {@code complete="true"}, the appender outputs a well-formed YAML document. By default, with
 * {@code complete="false"}, you should include the output as an <em>external file</em> in a separate file to form a
 * well-formed YAML document.
 * </p>
 * <p>
 * A well-formed YAML event follows this pattern:
 * </p>
 *
 * <pre>
 * 
 * </pre>
 * <p>
 * If {@code complete="false"}, the appender does not write the YAML open array character "[" at the start of the
 * document, "]" and the end, nor comma "," between records.
 * </p>
 * <p>
 * This approach enforces the independence of the YamlLayout and the appender where you embed it.
 * </p>
 * <h3>Encoding</h3>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise
 * events containing non ASCII characters could result in corrupted log files.
 * </p>
 * <h3>Pretty vs. compact YAML</h3>
 * <p>
 * By default, the YAML layout is not compact (a.k.a. "pretty") with {@code compact="false"}, which means the appender
 * uses end-of-line characters and indents lines to format the text. If {@code compact="true"}, then no end-of-line or
 * indentation is used. Message content may contain, of course, escaped end-of-lines.
 * </p>
 */
@Plugin(name = "JsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class YamlLayout extends AbstractJacksonLayout {

    private static final String DEFAULT_FOOTER = ""; // TODO maybe

    private static final String DEFAULT_HEADER = ""; // TODO maybe

    static final String CONTENT_TYPE = "application/yaml";

    protected YamlLayout(final Configuration config, final boolean locationInfo, final boolean properties,
            final boolean complete, final boolean compact, final boolean eventEol, final String headerPattern,
            final String footerPattern, final Charset charset) {
        super(config, new JacksonFactory.YAML().newWriter(locationInfo, properties, compact), charset, compact,
                complete, eventEol,
                PatternLayout.createSerializer(config, null, headerPattern, DEFAULT_HEADER, null, false, false),
                PatternLayout.createSerializer(config, null, footerPattern, DEFAULT_FOOTER, null, false, false));
    }

    /**
     * Returns appropriate YAML header.
     *
     * @return a byte array containing the header, opening the YAML array.
     */
    @Override
    public byte[] getHeader() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        final String str = serializeToString(getHeaderSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    /**
     * Returns appropriate YAML footer.
     *
     * @return a byte array containing the footer, closing the YAML array.
     */
    @Override
    public byte[] getFooter() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(this.eol);
        final String str = serializeToString(getFooterSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        result.put("version", "2.0");
        return result;
    }

    @Override
    /**
     * @return The content type.
     */
    public String getContentType() {
        return CONTENT_TYPE + "; charset=" + this.getCharset();
    }

    /**
     * Creates a YAML Layout.
     * 
     * @param config
     *            The plugin configuration.
     * @param locationInfo
     *            If "true", includes the location information in the generated YAML.
     * @param properties
     *            If "true", includes the thread context in the generated YAML.
     * @param headerPattern
     *            The header pattern, defaults to {@code ""} if null.
     * @param footerPattern
     *            The header pattern, defaults to {@code ""} if null.
     * @param footerPattern
     * @param charset
     *            The character set to use, if {@code null}, uses "UTF-8".
     * @return A YAML Layout.
     */
    @PluginFactory
    public static AbstractJacksonLayout createLayout(
            // @formatter:off
            @PluginConfiguration final Configuration config,
            @PluginAttribute(value = "locationInfo", defaultBoolean = false) final boolean locationInfo,
            @PluginAttribute(value = "properties", defaultBoolean = false) final boolean properties,
            @PluginAttribute(value = "header", defaultString = DEFAULT_HEADER) final String headerPattern,
            @PluginAttribute(value = "footer", defaultString = DEFAULT_FOOTER) final String footerPattern,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset
            // @formatter:on
    ) {
        return new YamlLayout(config, locationInfo, properties, false, false, true, headerPattern, footerPattern,
                charset);
    }

    /**
     * Creates a YAML Layout using the default settings. Useful for testing.
     *
     * @return A YAML Layout.
     */
    public static AbstractJacksonLayout createDefaultLayout() {
        return new YamlLayout(new DefaultConfiguration(), false, false, false, false, false, DEFAULT_HEADER,
                DEFAULT_FOOTER, StandardCharsets.UTF_8);
    }

    @Override
    public void toSerializable(final LogEvent event, final Writer writer) throws IOException {
        if (complete && eventCount > 0) {
            writer.append(", ");
        }
        super.toSerializable(event, writer);
    }
}
