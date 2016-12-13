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
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.StringEncoder;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Abstract base class for Layouts that result in a String.
 * <p>
 * Since 2.4.1, this class has custom logic to convert ISO-8859-1 or US-ASCII Strings to byte[] arrays to improve
 * performance: all characters are simply cast to bytes.
 * </p>
 */
/*
 * Implementation note: prefer String.getBytes(String) to String.getBytes(Charset) for performance reasons. See
 * https://issues.apache.org/jira/browse/LOG4J2-935 for details.
 */
public abstract class AbstractStringLayout extends AbstractLayout<String> implements StringLayout {

    public abstract static class Builder<B extends Builder<B>> extends AbstractLayout.Builder<B> {
        
        @PluginBuilderAttribute(value = "charset")
        private Charset charset;

        @PluginElement("footerSerializer")
        private Serializer footerSerializer;

        @PluginElement("headerSerializer")
        private Serializer headerSerializer;

        public Charset getCharset() {
            return charset;
        }

        public Serializer getFooterSerializer() {
            return footerSerializer;
        }

        public Serializer getHeaderSerializer() {
            return headerSerializer;
        }

        public B setCharset(Charset charset) {
            this.charset = charset;
            return asBuilder();
        }

        public B setFooterSerializer(Serializer footerSerializer) {
            this.footerSerializer = footerSerializer;
            return asBuilder();
        }

        public B setHeaderSerializer(Serializer headerSerializer) {
            this.headerSerializer = headerSerializer;
            return asBuilder();
        }
        
    }
    
    public interface Serializer {
        String toSerializable(final LogEvent event);
    }

    /**
     * Variation of {@link Serializer} that avoids allocating temporary objects.
     * @since 2.6
     */
    public interface Serializer2 {
        StringBuilder toSerializable(final LogEvent event, final StringBuilder builder);
    }

    /**
     * Default length for new StringBuilder instances: {@value} .
     */
    protected static final int DEFAULT_STRING_BUILDER_SIZE = 1024;

    protected static final int MAX_STRING_BUILDER_SIZE = Math.max(DEFAULT_STRING_BUILDER_SIZE,
            size("log4j.layoutStringBuilder.maxSize", 2 * 1024));

    private static final ThreadLocal<StringBuilder> threadLocal = new ThreadLocal<>();

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
        trimToMaxSize(result);
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

    private static int size(final String property, final int defaultValue) {
        return PropertiesUtil.getProperties().getIntegerProperty(property, defaultValue);
    }

    protected static void trimToMaxSize(final StringBuilder stringBuilder) {
        if (stringBuilder.length() > MAX_STRING_BUILDER_SIZE) {
            stringBuilder.setLength(MAX_STRING_BUILDER_SIZE);
            stringBuilder.trimToSize();
        }
    }

    private Encoder<StringBuilder> textEncoder;
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
     * @param aCharset the charset used to encode the header bytes, footer bytes and anything else that needs to be
     *      converted from strings to bytes.
     * @param header the header bytes
     * @param footer the footer bytes
     */
    protected AbstractStringLayout(final Charset aCharset, final byte[] header, final byte[] footer) {
        super(null, header, footer);
        this.headerSerializer = null;
        this.footerSerializer = null;
        this.charset = aCharset == null ? StandardCharsets.UTF_8 : aCharset;
        this.charsetName = this.charset.name();
        useCustomEncoding = isPreJava8()
                && (StandardCharsets.ISO_8859_1.equals(aCharset) || StandardCharsets.US_ASCII.equals(aCharset));
        textEncoder = Constants.ENABLE_DIRECT_ENCODERS ? new StringBuilderEncoder(charset) : null;
    }

    /**
     * Builds a new layout.
     * @param config the configuration
     * @param aCharset the charset used to encode the header bytes, footer bytes and anything else that needs to be
     *      converted from strings to bytes.
     * @param headerSerializer the header bytes serializer
     * @param footerSerializer the footer bytes serializer
     */
    protected AbstractStringLayout(final Configuration config, final Charset aCharset,
            final Serializer headerSerializer, final Serializer footerSerializer) {
        super(config, null, null);
        this.headerSerializer = headerSerializer;
        this.footerSerializer = footerSerializer;
        this.charset = aCharset == null ? StandardCharsets.UTF_8 : aCharset;
        this.charsetName = this.charset.name();
        useCustomEncoding = isPreJava8()
                && (StandardCharsets.ISO_8859_1.equals(aCharset) || StandardCharsets.US_ASCII.equals(aCharset));
        textEncoder = Constants.ENABLE_DIRECT_ENCODERS ? new StringBuilderEncoder(charset) : null;
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

    private DefaultLogEventFactory getLogEventFactory() {
        return DefaultLogEventFactory.getInstance();
    }

    /**
     * Returns a {@code Encoder<StringBuilder>} that this Layout implementation can use for encoding log events.
     *
     * @return a {@code Encoder<StringBuilder>}
     */
    protected Encoder<StringBuilder> getStringBuilderEncoder() {
        if (textEncoder == null) {
            textEncoder = new StringBuilderEncoder(getCharset());
        }
        return textEncoder;
    }

    protected byte[] serializeToBytes(final Serializer serializer, final byte[] defaultValue) {
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
        final LogEvent logEvent = getLogEventFactory().createEvent(rootLogger.getName(), null, Strings.EMPTY,
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

}
