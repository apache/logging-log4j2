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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.core.LogEvent;

/**
 * Abstract base class for Layouts that result in a String.
 */
/*
 * Implementation note: prefer String.getBytes(String) to String.getBytes(Charset) for performance reasons.
 * See https://issues.apache.org/jira/browse/LOG4J2-935 for details.
 */
public abstract class AbstractStringLayout extends AbstractLayout<String> {

    /**
     * Default length for new StringBuilder instances: {@value} .
     */
    protected static final int DEFAULT_STRING_BUILDER_SIZE = 1024;
    
    private final static ThreadLocal<StringBuilder> threadLocal = new ThreadLocal<>();

    private static final long serialVersionUID = 1L;

    /**
     * The charset for the formatted message.
     */
    // TODO: Charset is not serializable. Implement read/writeObject() ?
    private final Charset charset;
    private final String charsetName;

    protected AbstractStringLayout(final Charset charset) {
        this(charset, null, null);
    }

    protected AbstractStringLayout(final Charset charset, final byte[] header, final byte[] footer) {
        super(header, footer);
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        this.charsetName = this.charset.name();
    }

    /**
     * Converts a String to a byte[].
     * 
     * @param str if null, return null.
     * @param charset if null, use the default charset.
     * @return a byte[]
     */
    static byte[] toBytes(final String str, final Charset charset) {
        if (str != null) {
            final Charset actual = charset != null ? charset : Charset.defaultCharset();
            try { // LOG4J2-935: String.getBytes(String) gives better performance
                return str.getBytes(actual.name());
            } catch (UnsupportedEncodingException e) {
                return str.getBytes(actual);
            }
        }
        return null;
    }

    /**
     * Returns a {@code StringBuilder} that this Layout implementation can use to write the formatted log event to.
     * 
     * @return a {@code StringBuilder}
     */
    protected StringBuilder getStringBuilder() {
        StringBuilder result = threadLocal.get();
        if (result == null) {
            result = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            threadLocal.set(result);
        }
        result.setLength(0);
        return result;
    }

    protected byte[] getBytes(final String s) {
        try { // LOG4J2-935: String.getBytes(String) gives better performance
            return s.getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
            return s.getBytes(charset);
        }
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
     * @param event The Log Event.
     * @return The formatted event as a byte array.
     */
    @Override
    public byte[] toByteArray(final LogEvent event) {
        return getBytes(toSerializable(event));
    }

}
