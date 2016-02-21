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

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Abstract base class for Layouts.
 *
 * @param <T>
 *            The Class that the Layout will format the LogEvent into.
 */
public abstract class AbstractLayout<T extends Serializable> implements Layout<T>, Serializable {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private static final long serialVersionUID = 1L;

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
     *            The configuration
     * @param header
     *            The header to include when the stream is opened. May be null.
     * @param footer
     *            The footer to add when the stream is closed. May be null.
     */
    public AbstractLayout(final Configuration configuration, final byte[] header, final byte[] footer) {
        super();
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
     *
     * @param event the LogEvent to encode.
     * @param destination holds the ByteBuffer to write into.
     */
    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        final byte[] data = toByteArray(event);
        writeTo(data, 0, data.length, destination);
    }

    /**
     * Writes the specified data to the specified destination.
     *
     * @param data the data to write
     * @param offset where to start in the specified data array
     * @param length the number of bytes to write
     * @param destination the {@code ByteBufferDestination} to write to
     */
    public static void writeTo(final byte[] data, int offset, int length, final ByteBufferDestination destination) {
        int chunk = 0;
        ByteBuffer buffer = destination.getByteBuffer();
        do {
            if (length > buffer.remaining()) {
                buffer = destination.drain(buffer);
            }
            chunk = Math.min(length, buffer.remaining());
            buffer.put(data, offset, chunk);
            offset += chunk;
            length -= chunk;
        } while (length > 0);
    }
}
