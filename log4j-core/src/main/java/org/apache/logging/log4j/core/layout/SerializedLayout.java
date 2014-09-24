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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Formats a {@link LogEvent} in its Java serialized form.
 */
@Plugin(name = "SerializedLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class SerializedLayout extends AbstractLayout<LogEvent> {

    private static final long serialVersionUID = 1L;

    private static byte[] serializedHeader;

    static {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.close();
            serializedHeader = baos.toByteArray();
        } catch (final Exception ex) {
            LOGGER.error("Unable to generate Object stream header", ex);
        }
    }

    private SerializedLayout() {
        super(null, null);
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent} as a serialized byte array of the LogEvent object.
     *
     * @param event The LogEvent.
     * @return the formatted LogEvent.
     */
    @Override
    public byte[] toByteArray(final LogEvent event) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream oos = new PrivateObjectOutputStream(baos);
            try {
                oos.writeObject(event);
                oos.reset();
            } finally {
                oos.close();
            }
        } catch (final IOException ioe) {
            LOGGER.error("Serialization of LogEvent failed.", ioe);
        }
        return baos.toByteArray();
    }

    /**
     * Returns the LogEvent.
     *
     * @param event The Logging Event.
     * @return The LogEvent.
     */
    @Override
    public LogEvent toSerializable(final LogEvent event) {
        return event;
    }

    /**
     * Creates a SerializedLayout.
     * @return A SerializedLayout.
     */
    @PluginFactory
    public static SerializedLayout createLayout() {
        return new SerializedLayout();
    }

    @Override
    public byte[] getHeader() {
        return serializedHeader;
    }

    /**
     * SerializedLayout returns a binary stream.
     * @return The content type.
     */
    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    /**
     * The stream header will be written in the Manager so skip it here.
     */
    private class PrivateObjectOutputStream extends ObjectOutputStream {

        public PrivateObjectOutputStream(final OutputStream os) throws IOException {
            super(os);
        }

        @Override
        protected void writeStreamHeader() {
        }
    }
}
