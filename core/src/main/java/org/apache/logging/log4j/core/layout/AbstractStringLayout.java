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

import org.apache.logging.log4j.core.LogEvent;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Abstract base class for Layouts that result in a String.
 */
public abstract class AbstractStringLayout extends AbstractLayout<String> {

    /**
     * The charset of the formatted message.
     */
    private final Charset charset;

    private final StringEncoder encoder;

    protected AbstractStringLayout(final Charset charset) {
        this.charset = charset;
        boolean useClass = false;
        try {
            if (String.class.getMethod("getBytes", new Class[] {Charset.class}) != null) {
                useClass = true;
            }
        } catch (final NoSuchMethodException ex) {
            // Not JDK 6 or greater.
        }
        encoder = useClass ? new ClassEncoder() : new NameEncoder();
    }

    /**
     * Formats the Log Event as a byte array.
     *
     * @param event The Log Event.
     * @return The formatted event as a byte array.
     */
    public byte[] toByteArray(final LogEvent event) {
        return encoder.getBytes(toSerializable(event));
    }

    /**
     * @return The default content type for Strings.
     */
    public String getContentType() {
        return "text/plain";
    }

    protected Charset getCharset() {
        return charset;
    }

    /**
     * Encoder interface to support Java 5 and Java 6+.
     */
    private interface StringEncoder {

        byte[] getBytes(String str);
    }

    /**
     * JDK 6 or greater.
     */
    private class ClassEncoder implements StringEncoder {
        public byte[] getBytes(final String str) {
            return str.getBytes(charset);
        }
    }

    /**
     * JDK 5.
     */
    private class NameEncoder implements StringEncoder {
        public byte[] getBytes(final String str) {
            try {
                return str.getBytes(charset.name());
            } catch (final UnsupportedEncodingException ex) {
                // This shouldn't ever happen since an invalid Charset would never have been created.
                return str.getBytes();
            }
        }
    }
}
