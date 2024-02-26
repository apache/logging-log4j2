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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.CoreKeys;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.spi.recycler.Recycler;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Abstract base class for Layouts that result in a String.
 * <p>
 * Since 2.4.1, this class has custom logic to convert ISO-8859-1 or US-ASCII Strings to byte[] arrays to improve
 * performance: all characters are simply cast to bytes.
 * </p>
 */
public abstract class AbstractStringLayout extends AbstractLayout implements StringLayout {

    public abstract static class Builder<B extends Builder<B>> extends AbstractLayout.Builder<B> {

        @PluginElement("footerSerializer")
        private Serializer footerSerializer;

        @PluginElement("headerSerializer")
        private Serializer headerSerializer;

        public Serializer getFooterSerializer() {
            return footerSerializer;
        }

        public Serializer getHeaderSerializer() {
            return headerSerializer;
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

    public interface Serializer extends Serializer2 {

        String toSerializable(final LogEvent event);

        default boolean requiresLocation() {
            return false;
        }

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

    protected static final int MAX_STRING_BUILDER_SIZE = Math.max(
            DEFAULT_STRING_BUILDER_SIZE,
            PropertyEnvironment.getGlobal().getProperty(CoreKeys.GC.class).layoutStringBuilderMaxSize());

    protected static Recycler<StringBuilder> createStringBuilderRecycler(final RecyclerFactory recyclerFactory) {
        return recyclerFactory.create(() -> new StringBuilder(DEFAULT_STRING_BUILDER_SIZE), stringBuilder -> {
            StringBuilders.trimToMaxSize(stringBuilder, MAX_STRING_BUILDER_SIZE);
            stringBuilder.setLength(0);
        });
    }

    private final Serializer footerSerializer;

    private final Serializer headerSerializer;

    protected final Recycler<Encoder<StringBuilder>> stringBuilderEncoderRecycler;

    protected final Recycler<StringBuilder> stringBuilderRecycler;

    protected AbstractStringLayout(final Configuration configuration, final Charset charset) {
        this(configuration, charset, null, (byte[]) null);
    }

    /**
     * Builds a new layout.
     * @param configuration a configuration
     * @param header the header bytes
     * @param footer the footer bytes
     */
    protected AbstractStringLayout(
            final Configuration configuration, final Charset charset, final byte[] header, final byte[] footer) {
        super(configuration, charset, header, footer);
        this.headerSerializer = null;
        this.footerSerializer = null;
        final RecyclerFactory recyclerFactory = configuration.getRecyclerFactory();
        this.stringBuilderEncoderRecycler = createStringBuilderEncoderRecycler(recyclerFactory, getCharset());
        this.stringBuilderRecycler = createStringBuilderRecycler(recyclerFactory);
    }

    /**
     * Builds a new layout.
     * @param configuration a configuration
     * @param charset the charset used to encode the header bytes, footer bytes and anything else that needs to be
     *      converted from strings to bytes.
     * @param headerSerializer the header bytes serializer
     * @param footerSerializer the footer bytes serializer
     */
    protected AbstractStringLayout(
            final Configuration configuration,
            final Charset charset,
            final Serializer headerSerializer,
            final Serializer footerSerializer) {
        super(configuration, charset, null, null);
        this.headerSerializer = headerSerializer;
        this.footerSerializer = footerSerializer;
        this.stringBuilderEncoderRecycler =
                createStringBuilderEncoderRecycler(configuration.getRecyclerFactory(), getCharset());
        this.stringBuilderRecycler = createStringBuilderRecycler(configuration.getRecyclerFactory());
    }

    private static Recycler<Encoder<StringBuilder>> createStringBuilderEncoderRecycler(
            final RecyclerFactory recyclerFactory, final Charset charset) {
        return recyclerFactory.create(() -> new StringBuilderEncoder(charset));
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

    protected byte[] serializeToBytes(final Serializer serializer, final byte[] defaultValue) {
        final String serializable = serializeToString(serializer);
        if (serializable == null) {
            return defaultValue;
        }
        final Charset charset = getCharset();
        return serializable.getBytes(charset != null ? charset : Charset.defaultCharset());
    }

    protected String serializeToString(final Serializer serializer) {
        if (serializer == null) {
            return null;
        }
        final Configuration config = getConfiguration();
        if (config == null) {
            return null;
        }
        final LoggerConfig rootLogger = config.getRootLogger();
        final LogEventFactory logEventFactory = config.getComponent(LogEventFactory.KEY);
        final String fqcn = getClass().getName();
        final LogEvent logEvent =
                logEventFactory.createEvent(rootLogger.getName(), null, fqcn, rootLogger.getLevel(), null, null, null);
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

    @Override
    public abstract String toSerializable(LogEvent event);
}
