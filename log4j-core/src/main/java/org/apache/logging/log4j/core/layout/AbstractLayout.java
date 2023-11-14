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
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;

/**
 * Abstract base class for Layouts.
 */
public abstract class AbstractLayout implements StringLayout {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * Subclasses can extend this abstract Builder.
     *
     * @param <B> The type to build.
     */
    public abstract static class Builder<B extends Builder<B>> {

        @PluginConfiguration
        private Configuration configuration;

        @PluginBuilderAttribute
        private Charset charset;

        @PluginBuilderAttribute
        private byte[] footer;

        @PluginBuilderAttribute
        private byte[] header;

        public B asBuilder() {
            return Cast.cast(this);
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public Charset getCharset() {
            return charset;
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

        public B setCharset(final Charset charset) {
            this.charset = charset;
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
     * The character set used for encoding log events.
     */
    protected final Charset charset;

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
     * @param configuration a configuration
     * @param charset a character set used for encoding log events; if null, UTF-8 will be used
     * @param header a header to include when the stream is opened, may be null
     * @param footer the footer to add when the stream is closed, may be null
     */
    public AbstractLayout(
            final Configuration configuration,
            final Charset charset,
            final byte[] header,
            final byte[] footer) {
        super();
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.charset = charset != null ? charset : DEFAULT_CHARSET;
        this.header = header;
        this.footer = footer;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public Map<String, String> getContentFormat() {
        return Map.of();
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
     * <pre>{@code
     * &#064;Category(Node.CATEGORY)
     * &#064;Plugin(value = "MyLayout", elementType = Layout.ELEMENT_TYPE, printObject = true)
     * public final class MyLayout extends AbstractStringLayout {
     *     &#064;Override
     *     public void encode(LogEvent event, ByteBufferDestination destination) {
     *         StringBuilder text = stringBuilderRecycler.acquire();
     *         try {
     *             convertLogEventToText(event, text);
     *             StringBuilderEncoder encoder = stringBuilderEncoderRecycler.acquire();
     *             try {
     *                 encoder.encode(text, destination);
     *             } finally {
     *                 stringBuilderEncoderRecycler.release(encoder);
     *             }
     *         } finally {
     *             stringBuilderRecycler.release(text);
     *         }
     *     }
     *
     *     private void convertLogEventToText(LogEvent event, StringBuilder destination) {
     *         ... // append a text representation of the log event to the StringBuilder
     *     }
     * }
     * }</pre>
     *
     * @param event the LogEvent to encode.
     * @param destination holds the ByteBuffer to write into.
     */
    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        final byte[] data = toByteArray(event);
        destination.writeBytes(data, 0, data.length);
    }

}
