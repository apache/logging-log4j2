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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.Encoder;

/**
 * Converts {@link LogEvent} instances into different layouts of data. A layout typically encodes into either
 * a {@link String} or {@code byte[]}. Since version 2.6, layouts implement {@link Encoder}
 * to support direct encoding of a log event into a {@link ByteBufferDestination} without creating temporary
 * intermediary objects. Since version 3.0.0, layouts no longer reference the legacy Java serialization API
 * and are limited to strings or bytes.
 */
public interface Layout extends Encoder<LogEvent> {

    /**
     * Main {@linkplain org.apache.logging.log4j.plugins.Configurable#elementType() plugin element type} for
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
     * Formats the event into bytes. This should use the configured character set.
     *
     * @param event The Logging Event.
     * @return The formatted event.
     */
    byte[] toByteArray(LogEvent event);

    /**
     * Returns the character set used for encoding log events.
     */
    default Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    /**
     * Serializes the log event into a String.
     *
     * @param event The Logging Event.
     * @return The formatted event.
     */
    String toSerializable(LogEvent event);

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

    /**
     * Indicates whether this Layout requires location information.
     * @return returns true if the Layout requires location information.
     */
    default boolean requiresLocation() {
        return false;
    }
}
