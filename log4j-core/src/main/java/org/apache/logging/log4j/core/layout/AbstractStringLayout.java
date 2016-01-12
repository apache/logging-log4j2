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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.util.StringEncoder;
import org.apache.logging.log4j.util.Strings;

/**
 * Abstract base class for Layouts that result in a String.
 * <p>
 * Since 2.4.1, this class has custom logic to convert ISO-8859-1 or US-ASCII Strings to byte[] arrays to improve
 * performance: all characters are simply cast to bytes.
 */
/*
 * Implementation note: prefer String.getBytes(String) to String.getBytes(Charset) for performance reasons. See
 * https://issues.apache.org/jira/browse/LOG4J2-935 for details.
 */
public abstract class AbstractStringLayout extends AbstractLayout<String> implements StringLayout {

    public interface Serializer {
        
        String toSerializable(final LogEvent event);        
    }

    /**
     * Default length for new StringBuilder instances: {@value} .
     */
    protected static final int DEFAULT_STRING_BUILDER_SIZE = 1024;

    private static final long serialVersionUID = 1L;

    private final static ThreadLocal<StringBuilder> threadLocal = new ThreadLocal<>();

    /**
     * Returns a {@code StringBuilder} that this Layout implementation can use to write the formatted log event to.
     * 
     * @return a {@code StringBuilder}
     */
    protected static StringBuilder getStringBuilder() {
        StringBuilder result = threadLocal.get();
        if (result == null) {
            result = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            threadLocal.set(result);
        }
        result.setLength(0);
        return result;
    }
    // LOG4J2-1151: If the built-in JDK 8 encoders are available we should use them.
    private static boolean isPreJava8() {
        final String version = System.getProperty("java.version");
        final String[] parts = version.split("\\.");
        try {
            final int major = Integer.parseInt(parts[1]);
            return major < 8;
        } catch (final Exception ex) {
            return true;
        }
    }
    /**
     * The charset for the formatted message.
     */
    // LOG4J2-1099: charset cannot be final due to serialization needs, so we serialize as charset name instead
    private transient Charset charset;

    private final String charsetName;

    private final Serializer footerSerializer;

    private final Serializer headerSerializer;

    private final boolean useCustomEncoding;

    protected AbstractStringLayout(final Charset charset) {
        this(charset, (byte[]) null, (byte[]) null);
    }
    
    /**
     * Builds a new layout.
     * @param charset the charset used to encode the header bytes, footer bytes and anything else that needs to be 
     *      converted from strings to bytes.
     * @param header the header bytes
     * @param footer the footer bytes
     */
    protected AbstractStringLayout(final Charset charset, final byte[] header, final byte[] footer) {
        super(null, header, footer);
        this.headerSerializer = null;
        this.footerSerializer = null;
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        this.charsetName = this.charset.name();
        useCustomEncoding = isPreJava8()
                && (StandardCharsets.ISO_8859_1.equals(charset) || StandardCharsets.US_ASCII.equals(charset));
    }

    /**
     * Builds a new layout.
     * @param Configuration config the configuration
     * @param charset the charset used to encode the header bytes, footer bytes and anything else that needs to be 
     *      converted from strings to bytes.
     * @param header the header bytes serializer
     * @param footer the footer bytes serializer
     */
    protected AbstractStringLayout(final Configuration config, final Charset charset, final Serializer headerSerializer, final Serializer footerSerializer) {
        super(config, null, null);
        this.headerSerializer = headerSerializer;
        this.footerSerializer = footerSerializer;
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        this.charsetName = this.charset.name();
        useCustomEncoding = isPreJava8()
                && (StandardCharsets.ISO_8859_1.equals(charset) || StandardCharsets.US_ASCII.equals(charset));
    }

    protected byte[] getBytes(final String s) {
        if (useCustomEncoding) { // rely on branch prediction to eliminate this check if false
            return StringEncoder.encodeSingleByteChars(s);
        }
        try { // LOG4J2-935: String.getBytes(String) gives better performance
            return s.getBytes(charsetName);
        } catch (final UnsupportedEncodingException e) {
            return s.getBytes(charset);
        }
    }

    @Override
    public Charset getCharset() {
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
     * Returns the footer, if one is available.
     * 
     * @return A byte array containing the footer.
     */
    @Override
    public byte[] getFooter() {
        return serializeToBytes(footerSerializer, super.getFooter());
    }

    public Serializer getFooterSerializer() {
        return footerSerializer;
    }

    /**
     * Returns the header, if one is available.
     * 
     * @return A byte array containing the header.
     */
    @Override
    public byte[] getHeader() {
        return serializeToBytes(headerSerializer, super.getHeader());
    }

    public Serializer getHeaderSerializer() {
        return headerSerializer;
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final String csName = in.readUTF();
        charset = Charset.forName(csName);
    }

    protected byte[] serializeToBytes(final Serializer serializer, byte[] defaultValue) {
        final String serializable = serializeToString(serializer);
        if (serializer == null) {
            return defaultValue;
        }
        return StringEncoder.toBytes(serializable, getCharset());
    }

    protected String serializeToString(final Serializer serializer) {
        if (serializer == null) {
            return null;
        }
        final LoggerConfig rootLogger = getConfiguration().getRootLogger();
        // Using "" for the FQCN, does it matter?
        final LogEvent logEvent = rootLogger.getLogEventFactory().createEvent(rootLogger.getName(), null, Strings.EMPTY,
                rootLogger.getLevel(), null, null, null);
        return serializer.toSerializable(logEvent);
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
    
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(charset.name());
    }

}
