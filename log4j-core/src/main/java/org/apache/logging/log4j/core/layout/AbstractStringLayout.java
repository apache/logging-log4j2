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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.impl.LocationAware;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.StringEncoder;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

/**
 * Abstract base class for Layouts that result in a String.
 * <p>
 * Since 2.4.1, this class has custom logic to convert ISO-8859-1 or US-ASCII Strings to byte[] arrays to improve
 * performance: all characters are simply cast to bytes.
 * </p>
 */
public abstract class AbstractStringLayout extends AbstractLayout<String> implements StringLayout, LocationAware {

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

        public B setCharset(final Charset charset) {
            this.charset = charset;
            return asBuilder();
        }

        public B setFooterSerializer(final Serializer footerSerializer) {
            this.footerSerializer = footerSerializer;
            return asBuilder();
        }

        public B setHeaderSerializer(final Serializer headerSerializer) {
            this.headerSerializer = headerSerializer;
            return asBuilder();
        }
    }

    @Override
    public boolean requiresLocation() {
        return false;
    }

    public interface Serializer extends Serializer2 {
        String toSerializable(final LogEvent event);

        @Override
        default StringBuilder toSerializable(final LogEvent event, final StringBuilder builder) {
            builder.append(toSerializable(event));
            return builder;
        }
    }

    /**
     * Variation of {@link Serializer} that avoids allocating temporary objects.
     * As of 2.13 this interface was merged into the Serializer interface.
     * @since 2.6
     */
    public interface Serializer2 {
        StringBuilder toSerializable(final LogEvent event, final StringBuilder builder);
    }

    /**
     * Default length for new StringBuilder instances: {@value} .
     */
    protected static final int DEFAULT_STRING_BUILDER_SIZE = 1024;

    protected static final int MAX_STRING_BUILDER_SIZE =
            Math.max(DEFAULT_STRING_BUILDER_SIZE, size("log4j.layoutStringBuilder.maxSize", 2 * 1024));

    private static final ThreadLocal<StringBuilder> threadLocal = new ThreadLocal<>();

    /**
     * Returns a {@code StringBuilder} that this Layout implementation can use to write the formatted log event to.
     *
     * @return a {@code StringBuilder}
     */
    protected static StringBuilder getStringBuilder() {
        if (AbstractLogger.getRecursionDepth() > 1) { // LOG4J2-2368
            // Recursive logging may clobber the cached StringBuilder.
            return new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
        }
        StringBuilder result = threadLocal.get();
        if (result == null) {
            result = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            threadLocal.set(result);
        }
        trimToMaxSize(result);
        result.setLength(0);
        return result;
    }

    private static int size(final String property, final int defaultValue) {
        return PropertiesUtil.getProperties().getIntegerProperty(property, defaultValue);
    }

    protected static void trimToMaxSize(final StringBuilder stringBuilder) {
        StringBuilders.trimToMaxSize(stringBuilder, MAX_STRING_BUILDER_SIZE);
    }

    private Encoder<StringBuilder> textEncoder;
    /**
     * The charset for the formatted message.
     */
    private final Charset charset;

    private final Serializer footerSerializer;

    private final Serializer headerSerializer;

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
        textEncoder = Constants.ENABLE_DIRECT_ENCODERS ? new StringBuilderEncoder(charset) : null;
    }

    /**
     * Builds a new layout.
     * @param config the configuration. May be null.
     * @param aCharset the charset used to encode the header bytes, footer bytes and anything else that needs to be
     *      converted from strings to bytes.
     * @param headerSerializer the header bytes serializer
     * @param footerSerializer the footer bytes serializer
     */
    protected AbstractStringLayout(
            final Configuration config,
            final Charset aCharset,
            final Serializer headerSerializer,
            final Serializer footerSerializer) {
        super(config, null, null);
        this.headerSerializer = headerSerializer;
        this.footerSerializer = footerSerializer;
        this.charset = aCharset == null ? StandardCharsets.UTF_8 : aCharset;
        textEncoder = Constants.ENABLE_DIRECT_ENCODERS ? new StringBuilderEncoder(charset) : null;
    }

    protected byte[] getBytes(final String s) {
        return s.getBytes(charset);
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
        if (serializable == null) {
            return defaultValue;
        }
        return StringEncoder.toBytes(serializable, getCharset());
    }

    protected String serializeToString(final Serializer serializer) {
        if (serializer == null) {
            return null;
        }
        final String loggerName;
        final Level level;
        if (configuration != null) {
            final LoggerConfig rootLogger = configuration.getRootLogger();
            loggerName = rootLogger.getName();
            level = rootLogger.getLevel();
        } else {
            loggerName = LogManager.ROOT_LOGGER_NAME;
            level = AbstractConfiguration.getDefaultLevel();
        }
        // Using "" for the FQCN, does it matter?
        final LogEvent logEvent =
                getLogEventFactory().createEvent(loggerName, null, Strings.EMPTY, level, null, null, null);
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
