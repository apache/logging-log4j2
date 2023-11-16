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
package org.apache.logging.log4j.core;

import java.io.Serializable;
import java.util.Map;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.Encoder;

/**
 * Lays out a {@linkplain LogEvent} in different formats.
 *
 * The formats are:
 * <ul>
 * <li>
 * {@code byte[]}</li>
 * <li>
 * an implementer of {@linkplain Serializable}, like {@code byte[]}</li>
 * <li>
 * {@linkplain String}</li>
 * <li>
 * {@linkplain LogEvent}</li>
 * </ul>
 * <p>
 * Since 2.6, Layouts can {@linkplain Encoder#encode(Object, ByteBufferDestination) encode} a {@code LogEvent} directly
 * to a {@link ByteBufferDestination} without creating temporary intermediary objects.
 * </p>
 *
 * @param <T>
 *            The {@link Serializable} type returned by {@link #toSerializable(LogEvent)}
 */
public interface Layout<T extends Serializable> extends Encoder<LogEvent> {

    /**
     * Main {@linkplain org.apache.logging.log4j.core.config.plugins.Plugin#elementType() plugin element type} for
     * Layout plugins.
     *
     * @since 2.1
     */
    String ELEMENT_TYPE = "layout";

    /**
     * Returns the format for the layout format.
     * @return The footer.
     */
    byte[] getFooter();

    /**
     * Returns the header for the layout format.
     * @return The header.
     */
    byte[] getHeader();

    /**
     * Formats the event suitable for display.
     *
     * @param event The Logging Event.
     * @return The formatted event.
     */
    byte[] toByteArray(LogEvent event);

    /**
     * Formats the event as an Object that can be serialized.
     *
     * @param event The Logging Event.
     * @return The formatted event.
     */
    T toSerializable(LogEvent event);

    /**
     * Returns the content type output by this layout. The base class returns "text/plain".
     *
     * @return the content type.
     */
    String getContentType();

    /**
     * Returns a description of the content format.
     *
     * @return a Map of key/value pairs describing the Layout-specific content format, or an empty Map if no content
     * format descriptors are specified.
     */
    Map<String, String> getContentFormat();
}
