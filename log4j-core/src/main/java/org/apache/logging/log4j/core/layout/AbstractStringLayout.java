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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Charsets;

/**
 * Abstract base class for Layouts that result in a String.
 */
public abstract class AbstractStringLayout extends AbstractLayout<String> {

    private static final long serialVersionUID = 1L;

    /**
     * The charset for the formatted message.
     */
    // TODO: Charset is not serializable. Implement read/writeObject() ?
    private final Charset charset;

    protected AbstractStringLayout(final Charset charset) {
        this(charset, null, null);
    }

    protected AbstractStringLayout(final Charset charset, final byte[] header, final byte[] footer) {
        super(header, footer);
        this.charset = charset == null ? Charsets.UTF_8 : charset;
    }

    protected byte[] getBytes(String s) {
        return s.getBytes(charset);
    }

    protected Charset getCharset() {
        return charset;
    }

    /**
     * @return The default content type for Strings.
     */
    @Override
    public String getContentType() {
        return "text/plain";
    }

    /**
     * Formats the Log Event as a byte array.
     *
     * @param event
     *        The Log Event.
     * @return The formatted event as a byte array.
     */
    @Override
    public byte[] toByteArray(final LogEvent event) {
        return toSerializable(event).getBytes(charset);
    }
}
