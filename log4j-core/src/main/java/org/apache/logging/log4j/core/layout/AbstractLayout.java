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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Abstract base class for Layouts.
 *
 * @param <T>
 *            The Class that the Layout will format the LogEvent into.
 */
public abstract class AbstractLayout<T extends Serializable> implements Layout<T> {

    /**
     * Subclasses can extend this abstract Builder.
     *
     * @param <B> The type to build.
     */
    public abstract static class Builder<B extends Builder<B>> {

        @PluginConfiguration
        private Configuration configuration;

        @PluginBuilderAttribute
        private byte[] footer;

        @PluginBuilderAttribute
        private byte[] header;

        @SuppressWarnings("unchecked")
        public B asBuilder() {
            return (B) this;
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public byte[] getFooter() {
            return footer;
        }

        public byte[] getHeader() {
            return header;
        }

        public B setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return asBuilder();
        }

        public B setFooter(final byte[] footer) {
            this.footer = footer;
            return asBuilder();
        }

        public B setHeader(final byte[] header) {
            this.header = header;
            return asBuilder();
        }
    }

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * The current Configuration.
     */
    protected final Configuration configuration;

    /**
     * The number of events successfully processed by this layout.
     */
    protected long eventCount;

    /**
     * The footer to add when the stream is closed. May be null.
     */
    protected final byte[] footer;

    /**
     * The header to include when the stream is opened. May be null.
     */
    protected final byte[] header;

    /**
     * Constructs a layout with an optional header and footer.
     *
     * @param header
     *            The header to include when the stream is opened. May be null.
     * @param footer
     *            The footer to add when the stream is closed. May be null.
     * @deprecated Use {@link #AbstractLayout(Configuration, byte[], byte[])}
     */
    @Deprecated
    public AbstractLayout(final byte[] header, final byte[] footer) {
        this(null, header, footer);
    }

    /**
     * Constructs a layout with an optional header and footer.
     *
     * @param configuration
     *            The configuration. May be null.
     * @param header
     *            The header to include when the stream is opened. May be null.
     * @param footer
     *            The footer to add when the stream is closed. May be null.
     */
    public AbstractLayout(final Configuration configuration, final byte[] header, final byte[] footer) {
        this.configuration = configuration;
        this.header = header;
        this.footer = footer;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Map<String, String> getContentFormat() {
        return new HashMap<>();
    }

    /**
     * Returns the footer, if one is available.
     *
     * @return A byte array containing the footer.
     */
    @Override
    public byte[] getFooter() {
        return footer;
    }

    /**
     * Returns the header, if one is available.
     *
     * @return A byte array containing the header.
     */
    @Override
    public byte[] getHeader() {
        return header;
    }

    protected void markEvent() {
        eventCount++;
    }

    /**
     * Encodes the specified source LogEvent to some binary representation and writes the result to the specified
     * destination.
     * <p>
     * The default implementation of this method delegates to the {@link #toByteArray(LogEvent)} method which allocates
     * temporary objects.
     * </p><p>
     * Subclasses can override this method to provide a garbage-free implementation. For text-based layouts,
     * {@code AbstractStringLayout} provides various convenience methods to help with this:
     * </p>
     * <pre> &#64;Plugin(name = "MyLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
     * public final class MyLayout extends AbstractStringLayout {
     *     &#64;Override
     *     public void encode(LogEvent event, ByteBufferDestination destination) {
     *         StringBuilder text = getStringBuilder();
     *         convertLogEventToText(event, text);
     *         getStringBuilderEncoder().encode(text, destination);
     *     }
     *
     *     private void convertLogEventToText(LogEvent event, StringBuilder destination) {
     *         ... // append a text representation of the log event to the StringBuilder
     *     }
     * }
     * </pre>
     *
     * @param event the LogEvent to encode.
     * @param destination holds the ByteBuffer to write into.
     * @see AbstractStringLayout#getStringBuilder()
     * @see AbstractStringLayout#getStringBuilderEncoder()
     */
    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        final byte[] data = toByteArray(event);
        destination.writeBytes(data, 0, data.length);
    }
}
